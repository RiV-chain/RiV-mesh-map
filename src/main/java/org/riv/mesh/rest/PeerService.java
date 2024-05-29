package org.riv.mesh.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.riv.mesh.api.PeerPortRequest;
import org.riv.node.PeerInfo;

import com.ip2location.IP2Location;
import com.ip2location.IPResult;

import listener.SchedulePeerReaderListener;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/peer")
public class PeerService extends BaseService {
	
	private static final String binfile = "IP2LOCATION-LITE-DB1.BIN";
	
	private static final IP2Location ip2countryDb;
	
	//FIX ME only TCP peers
	private static final String DEFAULT_PEER_SCHEMA = "tcp://";
	
	private static final int TIMEOUT_MS = 5000; // 5 seconds
	
	
	static {
		try {
			byte[] ip2countryDbBytes = Thread.currentThread().getContextClassLoader().getResourceAsStream(binfile).readAllBytes();
			ip2countryDb = new IP2Location();
			ip2countryDb.Open(ip2countryDbBytes);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("Cannot load ip2country db file.");
		}
	}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postPeer(PeerPortRequest peerPortRequest, @Context HttpServletRequest request) {
        // Extract port from the request body
        int port = peerPortRequest.getPort();

        // Retrieve the remote request IP address behind ssl proxy
        String remoteIp = request.getHeader("X-Forwarded-For");
        if(remoteIp == null) {
        	// Fallback to direct remote IP
        	remoteIp = request.getRemoteAddr();
        }
        
        // Log or process the port and remote IP as needed
        System.out.println("Received port: " + port + ", from IP: " + remoteIp);
        
        // If test success increment rank otherwise decrement it.
        try {
			if(!testPortAndMeta(remoteIp, port)) {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).build();
		}
        
        // Get country info from ip2country
        IPResult result;
        try {
			result = ip2countryDb.IPQuery(remoteIp);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
        if(result.getStatus() != "OK" || result.getCountryLong().length() < 2) {
        	return Response.status(Status.BAD_REQUEST).build();
        }
        String countryLong = getFirstTwoWords(result.getCountryLong()).toLowerCase();
        
        String countryKey = countryLong+".md";
        
        // Test new peer availability
        PeerInfo peerInfo = new PeerInfo(null, null);
        peerInfo.setUp(Boolean.TRUE);
        LinkedHashMap<String, PeerInfo> countryPeers;
        if(SchedulePeerReaderListener.userPeers.containsKey(countryKey)) {
        	countryPeers = SchedulePeerReaderListener.userPeers.get(countryKey);
        } else {
        	countryPeers = new LinkedHashMap<>();
        	synchronized (SchedulePeerReaderListener.userPeers) {
        		SchedulePeerReaderListener.userPeers.put(countryKey, countryPeers);
			}
        }
        // Create a Peer using input port and remote IP.
        String peerrKey = DEFAULT_PEER_SCHEMA + remoteIp + ":" + port;
        if(countryPeers.get(peerrKey) != null) {
        	// Return too many requests
        	return Response.status(Status.TOO_MANY_REQUESTS).build();
        }
        synchronized(countryPeers) {
        	countryPeers.put(peerrKey, peerInfo);
        }
        
        // Return a response
        return Response.status(Status.CREATED).build();
    }
    
    private static boolean testPortAndMeta(String remoteIp, int port) throws IOException {

        Socket pingSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        String meta = null;
        try {
            pingSocket = new Socket();
            pingSocket.connect(new InetSocketAddress(remoteIp, port), TIMEOUT_MS);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
            meta = in.readLine();
            System.out.println(meta);
            if(meta == null) {
            	return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
        	if(out != null) {
        		out.close();
        	}
        	if(in != null ) {
        		in.close();
        	}
        	if(pingSocket != null) {
        		pingSocket.close();
        	}
        }
        return meta.startsWith("meta");
    }
    
    private static String getFirstTwoWords(String countryLong) {
        String[] words = countryLong.trim().toLowerCase().split("\\s+");
        if (words.length >= 2) {
            return words[0] + "-" + words[1];
        } else {
            return countryLong.trim();
        }
    }
    
    public static void main(String[] args) throws IOException {
    		System.out.println(testPortAndMeta("localhost", 4040));
    }
}