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

package experiments;

import enums.PSSMeasurementTags;
import enums.PeerSelectionPolicy;
import enums.ViewPropagationPolicy;
import peerlets.PeerSamplingService;
import protopeer.Experiment;
import protopeer.NeighborManager;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
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
public class PeerSamplingServiceExperiment extends SimulatedExperiment{

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

    public static void main(String[] args) {
        Experiment.initEnvironment();
		PeerSamplingServiceExperiment experiment = new PeerSamplingServiceExperiment();
		experiment.init();
		PeerFactory peerFactory=new PeerFactory() {
			public Peer createPeer(int peerIndex, Experiment experiment) {
				Peer newPeer = new Peer(peerIndex);
				if (peerIndex == 0) {
					newPeer.addPeerlet(new BootstrapServer());
				}
				newPeer.addPeerlet(new NeighborManager());
				newPeer.addPeerlet(new SimpleConnector());
                                newPeer.addPeerlet(new BootstrapClient(experiment.getAddressToBindTo(0), new SimplePeerIdentifierGenerator()));
				newPeer.addPeerlet(new PeerSamplingService(c, H, S, peerSelectionPolicy, viewPropagationPolicy, T, A, B));
				return newPeer;
			}
		};
		experiment.initPeers(0,N,peerFactory);
		experiment.startPeers(0,N);

		//run the simulation
		experiment.runSimulation(Time.inSeconds(runDuration));

        ResultsIllustrator results=new ResultsIllustrator(experiment);

        System.out.println("*** RESULTS PER EPOCH ***\n");
		for (int epochNumber=0; epochNumber<=runDuration; epochNumber++) {
			results.printEpochNumber(epochNumber);
            results.printSumEpochMeasurement(epochNumber, PSSMeasurementTags.MESSAGE_ACTION_RECEIVED);
			results.printSumEpochMeasurement(epochNumber, PSSMeasurementTags.MESSAGE_REACTION_REVEIVED);
            results.printSumEpochMeasurement(epochNumber, PSSMeasurementTags.MESSAGE_ACTION_SENT);
            results.printSumEpochMeasurement(epochNumber, PSSMeasurementTags.MESSAGE_REACTION_SENT);
            results.printEpochInDegreeStDev(epochNumber);
            if(epochNumber==140)
                results.printEpochInDegreeNodesProportion(epochNumber);
            results.clearEpochLog(epochNumber);
            System.out.println();
        }
        System.out.println("\n***********************************************\n");
        System.out.println("\n*** GLOBAL RESULTS ***\n");
//        results.printSumAggregateMeasurement(PSSMeasurementTags.MESSAGE_ACTION);
//        results.printSumAggregateMeasurement(PSSMeasurementTags.MESSAGE_REACTION);
    }

}
