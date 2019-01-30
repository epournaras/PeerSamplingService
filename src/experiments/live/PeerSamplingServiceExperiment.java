/*
 * Copyright (C) 2015 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package experiments.live;

import java.util.ArrayList;

import enums.PSSMeasurementTags;
import enums.PeerSelectionPolicy;
import enums.ViewPropagationPolicy;
import peerlets.PeerSamplingService;
import protopeer.Experiment;
import protopeer.MainConfiguration;
import protopeer.NeighborManager;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.ZMQExperiment;
import protopeer.network.NetworkInterfaceFactory;
import protopeer.network.delayloss.DelayLossNetworkInterfaceFactory;
import protopeer.network.delayloss.UniformDelayModel;
import protopeer.servers.bootstrap.BootstrapClient;
import protopeer.servers.bootstrap.BootstrapServer;
import protopeer.servers.bootstrap.SimpleConnector;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import protopeer.util.quantities.Time;

/**
 *
 * @author Evangelos
 */
public class PeerSamplingServiceExperiment extends ZMQExperiment{

    private final static int runDuration=350;
    private final static int N=1000;
    private final static int c=50;
    private final static int H=0;
    private final static int S=25;
    private final static ViewPropagationPolicy viewPropagationPolicy=ViewPropagationPolicy.PUSHPULL;
    private final static PeerSelectionPolicy peerSelectionPolicy=PeerSelectionPolicy.RAND;
    private final static int T=1000;
    private final static int A=1000;
    private final static int B=6000;

//    @Override
//	public NetworkInterfaceFactory createNetworkInterfaceFactory() {
//		return new DelayLossNetworkInterfaceFactory(getEventScheduler(),new UniformDelayModel(0.15,2.5));
//	}

    public static void main(String[] args) 
    {
    	
    	System.out.printf("PeerSamplingServiceExperiment (2019-01-29)\n" );
    	
    	if (args.length != 2) 
		{
			System.err.printf("PeerSamplingServiceExperiment: usage: index port\n" );
            return;
        }
		
		final 	int		index = Integer.parseInt(args [0]),
		 				port = Integer.parseInt(args [1]);
		
		System.out.format( "index : %s, port : %s\n", index, port );
		
        Experiment.initEnvironment();
        
        
        
        // set configuration
        MainConfiguration.getSingleton().peerIndex = index;
     	MainConfiguration.getSingleton().peerPort = port;
     	
      		
		PeerSamplingServiceExperiment experiment = new PeerSamplingServiceExperiment();
		
		experiment.init();
		
		ArrayList<Peer> 	listPeers = 	new ArrayList<Peer>();
		
		PeerFactory peerFactory=new PeerFactory() 
		{
			public Peer createPeer(int peerIndex, Experiment experiment) 
			{
				Peer newPeer = new Peer(peerIndex);
				
				listPeers.add(newPeer);
				if (peerIndex == 0) 
				{
					// as in DIAS, the Bootstrap Server peer only has a single peerlet, the BootstrapServer
					// edward | 2019-01-29
					newPeer.addPeerlet(new BootstrapServer());
					System.out.format( "BootstrapServer created\n" );
				}
				else
				{
				
					newPeer.addPeerlet(new NeighborManager());
					
					newPeer.addPeerlet(new SimpleConnector());
	                                
					BootstrapClient	bsc = new BootstrapClient(experiment.getAddressToBindTo(0), new SimplePeerIdentifierGenerator());
					newPeer.addPeerlet(bsc);
					
	                PeerSamplingService pss = new PeerSamplingService(c, H, S, peerSelectionPolicy, viewPropagationPolicy, T, A, B);
	                newPeer.addPeerlet(pss);
	                
	                // kick off the bootstrap process
	                //bsc.SendBootstrapHello();
	                //System.out.format( "BootstrapHello sent\n" );
				}
				
                return newPeer;
			}
		};
		
		int myPeerIndex = MainConfiguration.getSingleton().peerIndex;
		experiment.initPeers(myPeerIndex, 1, peerFactory);
		experiment.startPeers(myPeerIndex, 1);
		System.out.println("Bound to: "
				+ experiment.getPeers().elementAt(myPeerIndex)
						.getNetworkAddress());
		
		// in DIAS, the BoostrapClient does not initially connect to the BoostrapServer
		// the connection only takes places when a device connects to the peer. at which point the BootstrapClient.SendBootstrapHello() method is called
		// as there are no devices nor DIAS in this simulation, we wait a while and then send the BootstrapHello message to the BoostrapServer
		// edward | 2019-01-29
		System.out.println( "Waiting for sending Bootstrap Hello");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for( Peer peer : listPeers)
		{
			if( peer.getIndexNumber() != 0 )
			{
				((BootstrapClient)peer.getPeerletOfType(BootstrapClient.class)).SendBootstrapHello();
				System.out.println( "Bootstrap Hello sent");
			}
		}
		
		
		
 
    }

}
