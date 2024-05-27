package org.riv.mesh.rest;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.riv.node.PeerInfo;

import listener.SchedulePeerReaderListener;

@Path("/peers.json")
public class PeersService extends BaseService {
	
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPeers() {
    	Map<String, LinkedHashMap<String, PeerInfo>> map = SchedulePeerReaderListener.mergedPeers;
        return Response.status(Status.OK).entity(gson.toJson(map)).build();
    }

}