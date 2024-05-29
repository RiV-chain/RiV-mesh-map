package listener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.riv.node.PeerInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SchedulePeerReaderListener implements ServletContextListener {
	
	private static final String PEERS_REPO_PATH = "/opt/tomcat/public-peers";//"D:\\Git\\public-peers";//
	private static final String PEERS_MERGED_PATH = "/opt/tomcat/merged-peers";

	/**
	 * Peers per Country
	 */
	public static final Map<String, LinkedHashMap<String, PeerInfo>> whiteListPeers = new TreeMap<String, LinkedHashMap<String, PeerInfo>>();

	public static Map<String, LinkedHashMap<String, PeerInfo>> mergedPeers = new TreeMap<String, LinkedHashMap<String, PeerInfo>>();
	
	public static final Map<String, LinkedHashMap<String, PeerInfo>> userPeers = new TreeMap<String, LinkedHashMap<String, PeerInfo>>();

	Pattern IPv4_PEER_REGEXP = Pattern.compile(
			"(((tcp)|(tls)|(sctp)|(mpath))\\://(((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d))\\:\\d{1,5})");
	Pattern IPv6_PEER_REGEXP = Pattern.compile(
			"((tcp)|(tls)|(sctp)|(mpath))\\://\\[(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))\\]\\:\\d{1,5}");
	Pattern DOMAIN_NAME_PEER_REGEXP = Pattern.compile("(((tcp)|(tls)|(sctp)|(mpath))\\://([\\w\\-]+\\.)+[a-zA-Z]+\\:\\d{1,5})");

	public static int period = 30;

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
		File peersMerged = new File(PEERS_MERGED_PATH+"/peers.json");
		if(peersMerged.exists()) {
			// Read to memory
			SchedulePeerReaderListener.mergedPeers = loadJsonFromFile(peersMerged);
		}
		
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);
		Runnable parseGitHubRepo = () -> {

			try {
				Iterator<File> it = FileUtils.iterateFilesAndDirs(new File(PEERS_REPO_PATH), new IOFileFilter() {

					@Override
					public boolean accept(File file) {
						return false;
					}

					@Override
					public boolean accept(File dir, String name) {
						return false;
					}

				}, TrueFileFilter.INSTANCE);
				//
				while (it.hasNext()) {
					File f = it.next();
					if (f.isDirectory()) {
						Iterator<File> country = FileUtils.iterateFiles(f,

								new IOFileFilter() {

									@Override
									public boolean accept(File file) {
										return file.getName().endsWith(".md");
									}

									@Override
									public boolean accept(File dir, String name) {
										return false;
									}

								}, null);
						while (country.hasNext()) {
							LinkedHashMap<String, PeerInfo> peerInfoMap = new LinkedHashMap<String, PeerInfo>();
							// parse peers in md file
							File countryNext = country.next();
							try {
								List<String> content = FileUtils.readLines(countryNext, "UTF-8");
								for (String c : content) {
									Matcher mIpv4 = IPv4_PEER_REGEXP.matcher(c);
									while (mIpv4.find()) {
										String peer = mIpv4.group(0);
										PeerInfo pi = new PeerInfo(null, null);
										peerInfoMap.put(peer, pi);
									}
									Matcher mIpv6 = IPv6_PEER_REGEXP.matcher(c);
									while (mIpv6.find()) {
										String peer = mIpv6.group(0);
										PeerInfo pi = new PeerInfo(null, null);
										peerInfoMap.put(peer, pi);
									}
									Matcher mDomain = DOMAIN_NAME_PEER_REGEXP.matcher(c);
									while (mDomain.find()) {
										String peer = mDomain.group(0);
										PeerInfo pi = new PeerInfo(null, null);
										peerInfoMap.put(peer, pi);
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
							if (peerInfoMap.size() > 0) {
								whiteListPeers.put(countryNext.getName(), peerInfoMap);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			mergeMaps(whiteListPeers, userPeers);
			whiteListPeers.clear();
	        userPeers.clear();
	      //Save peers
			// Convert mergedPeers to JSON string
			String data = new Gson().toJson(SchedulePeerReaderListener.mergedPeers);

	        // Save JSON string to file
	        boolean success = saveJsonToFile(data, PEERS_MERGED_PATH+"/peers.json");
	        if (success) {
	            System.out.println("Data saved successfully to peers.json");
	        } else {
	            System.out.println("Failed to save data to peers.json");
	        }
		};

		ses.scheduleAtFixedRate(parseGitHubRepo, 5, period , TimeUnit.SECONDS);
		arg0.getServletContext().setAttribute("timer", ses);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		ServletContext servletContext = arg0.getServletContext();
		ScheduledExecutorService ses = (ScheduledExecutorService) servletContext.getAttribute("timer");
		ses.shutdown();
	}
	
    public static void mergeMaps(
            Map<String, LinkedHashMap<String, PeerInfo>> whiteListPeers,
            Map<String, LinkedHashMap<String, PeerInfo>> userPeers) {
        Map<String, LinkedHashMap<String, PeerInfo>> mergedMap = new TreeMap<>(whiteListPeers);

        for (Map.Entry<String, LinkedHashMap<String, PeerInfo>> entry : userPeers.entrySet()) {
            mergedMap.merge(entry.getKey(), entry.getValue(), (existingValue, newValue) -> {
                existingValue.putAll(newValue);
                return existingValue;
            });
        }
        synchronized (mergedPeers) {
        	for (Map.Entry<String, LinkedHashMap<String, PeerInfo>> entry : mergedMap.entrySet()) {
                mergedPeers.merge(entry.getKey(), entry.getValue(), (existingValue, newValue) -> {
                    existingValue.putAll(newValue);
                    return existingValue;
                });
            }
        }
    }

    private static boolean saveJsonToFile(String jsonString, String fileName) {
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(jsonString);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static Map<String, LinkedHashMap<String, PeerInfo>> loadJsonFromFile(File file) {
        try (FileReader fileReader = new FileReader(file)) {
            Type type = new TypeToken<TreeMap<String, LinkedHashMap<String, PeerInfo>>>() {}.getType();
            return new Gson().fromJson(fileReader, type);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}