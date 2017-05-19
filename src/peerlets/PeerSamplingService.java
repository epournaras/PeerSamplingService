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

package peerlets;

import components.ViewManager;
import enums.PSSMeasurementTags;
import enums.MessageType;
import enums.PeerSelectionPolicy;
import enums.ViewPropagationPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import protopeer.BasePeerlet;
import protopeer.Peer;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.Finger;
import dsutil.protopeer.FingerDescriptor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import protopeer.NeighborManager;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.network.Message;
import protopeer.network.NetworkAddress;
import protopeer.util.quantities.Time;
import util.SwapMessage;

/**
 * This peerlet implements the Peer Samplign Service. This is a gossiping
 * framework that can be used for implementing a number of gossiping 
 * protocols. The implementation is based on the work presented in the
 * following paper:
 * 
 * M. Jelasity, S. Voulgaris, R. Guerraoui, A.-M. Kermarrec, M. van Steen., 
 * Gossip-based peer sampling, ACM Transactions on Computer Systems, August 2007
 * 
 * Other peerlets can access the Peer Sampling Service peerlet and request a 
 * random neighbor or the random view that exist the momment of the request.
 *
 * @author Evangelos
 */
public class PeerSamplingService extends BasePeerlet{

    private ViewManager viewManager;
    private final int c;
    private final int H;
    private final int S;
    private final PeerSelectionPolicy peerSelectionPolicy;
    private final ViewPropagationPolicy viewPropagationPolicy;
    private final int T;
    private final int A;
    private final int B;

    //measurement metrics
    private double actionsSent=0.0;
    private double reactionsSent=0.0;
    private double actionsReceived=0.0;
    private double reactionsReceived=0.0;
    private double parosMess=0.0;

    /**
	 * Initiates the peer sampling service. The systems is parameterized.
     *
     * @param push the information push activation or deactivation parameter
     * @param pull the information pull activation or deactivation parameter
     * @param c the length of the view
     * @param H the healing paramerer
     * @param S the swap parameter
     * @param peerSelectionPolicy the peer selection policy. It can be RAND or TAIL
     * @param T the period that the active state is triggered
     * @param A the period that the age fields of the descriptos is updated
     * @param B the bootstrapping initial period
	 */
    public PeerSamplingService(int c, int H, int S, PeerSelectionPolicy peerSelectionPolicy, ViewPropagationPolicy viewPropagationPolicy, int T, int A, int B){
        this.c=c;
        this.H=H;
        this.S=S;
        this.peerSelectionPolicy=peerSelectionPolicy;
        this.viewPropagationPolicy=viewPropagationPolicy;
        this.T=T;
        this.A=A;
        this.B=B;
   }

    /**
	 * Gets the initial neighbors from the <code>NeighborManager</code>. In this
     * case, it is used only for bootstrapping and the Peer Sampling Service
     * mainly uses the <code>ViewManager</code>.
     *
	 */
    private NeighborManager getNeighborManager() {
        return (NeighborManager) getPeer().getPeerletOfType(NeighborManager.class);
	}

    /**
	 * Inits the peer before starts.
     *
     * @param peer the initiated peer
     *
	 */
    @Override
    public void init(Peer peer){
        super.init(peer);
        
    }

    /**
	 * Starts the peer sampling service by initializing the view manager and
     * activating the bootstrapping process
	 */
    @Override
    public void start(){
        viewManager=new ViewManager(c, H, S, peerSelectionPolicy, getPeer().getFinger().clone());
        this.bootstrap();
    }

    /**
	 * Stops the Peer Sampling Service. NOTE: TODO
     *
	 */
    @Override
    public void stop(){

    }

    /**
	 * Returns a copy of the local descriptor
     *
	 */
    public FingerDescriptor getMyDescriptor(){
        return this.viewManager.getMyDescriptor().clone();
    }

    /**
	 * Registers a descriptor type in the local finger descriptor. In this way,
     * information about the fingers is disemminated through the peer sampling
     * service
     *
     * @param type the descriptor type
     * @param value the value of the descriptor
	 */
    public void registerDescriptor(Enum type, Object value){
        this.viewManager.getMyDescriptor().addDescriptor(type, value);
    }

    /**
	 * Unregisters a descriptor type in the local finger descriptor.
     *
     * @param type the descriptor type
	 */
    public void unregisterDescriptor(Enum type){
        this.viewManager.getMyDescriptor().removeDescriptor(type);
    }

    /**
	 * Returns a random finger descriptor that suppose to be a random sample
     * from the whole set of peers in the system. To increase randomness, the
     * method access a quee with samples in order not the same elements to be
     * returned.
	 */
    public FingerDescriptor getRandomFingerDescriptor(){
        Queue<FingerDescriptor> samples=this.viewManager.getSamples();
        if(samples.size()>0){
            return samples.poll().clone();
        }
        else{
            synchronized(this.viewManager.getView()){
                if(this.viewManager.getView().size()>0){
                    int position=(int)(Math.random()*this.viewManager.getView().size());
                    return this.viewManager.getView().get(position).clone();
                }
                return null;
            }

        }
    }

    /**
	 * Extracts the finger form the random returned descriptor.
	 */
    public Finger getRandomFinger(){
        return this.getRandomFingerDescriptor().getFinger();
    }

    /**
	 * Returns the fingers from the random view retained in the
     * <code>ViewManager</code>.
	 */
    public Collection<Finger> getRandomFingerView(){
        Collection<Finger> randFingers=new ArrayList<Finger>();
        synchronized(this.viewManager.getView()){
            for(FingerDescriptor descriptor:this.viewManager.getView()){
                randFingers.add(descriptor.getFinger().clone());
            }
        }
        
        return randFingers;
    }

    public Collection<FingerDescriptor> getRandomFingerDescriptorView(){
        Collection<FingerDescriptor> randFingersDescr=new ArrayList<FingerDescriptor>();
        synchronized(this.viewManager.getView()){
            for(FingerDescriptor descriptor:this.viewManager.getView()){
                randFingersDescr.add(descriptor.clone());
            }
        }
        return randFingersDescr;
    }

    /**
	 * Bootstraps and runs the system. We introduce an initial delay in order
     * the peers to get their neighbors from the <code>NeighborManager</code>.
     * Upon completion, the <code>ViewManager</code> is initiated, measurements
     * are sceduled and the events are triggered.
	 */
    private void bootstrap(){
        Timer bootstrapTimer= getPeer().getClock().createNewTimer();
        bootstrapTimer.addTimerListener(new TimerListener(){
            public void timerExpired(Timer timer){
                viewManager.setBootstrapPeers(getNeighborManager().getNeighbors());
                scheduleMeasurements();
                runActiveState();
            }
        });
        bootstrapTimer.schedule(B);
    }

    /**
	 * This is the active thread of the system. It triggers a swap in every
     * period T. Note that the trigerring time is an approximetations rather
     * than the specific time T. In this way, the central timer can work more
     * realisticly without problems of the sequance that the events are
     * processed. If the peer is in the PUSH state it will create the buffer
     * and push information. Otherwise it sends a buffer with no descriptors to
     * trigger an asnwer (REACTION). The method is recursive. The timer is sceduled
     * again after the time expires. The age is increased after the end of each
     * active state.
     *
	 */
    private void runActiveState(){
        Timer activeStateTimer=getPeer().getClock().createNewTimer();
        activeStateTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                FingerDescriptor neighbor=viewManager.selectPeer();
                if(neighbor!=null){
                    switch(viewPropagationPolicy){
                        case PUSHPULL:
                            sendBuffer(MessageType.ACTION, neighbor.getNetworkAddress());
                            break;
                        case PUSH:
                            SwapMessage message=new SwapMessage();
                            message.type=MessageType.ACTION;
                            message.buffer=new ArrayList();
                            getPeer().sendMessage(neighbor.getNetworkAddress(), message);
                            parosMess++;
                            break;
                        default:
                            //other view propagation policy
                    }
                    actionsSent=actionsSent+1.0;
                }
                viewManager.increaseAge(A);
                runActiveState(); //recursive call
            }
        });
        activeStateTimer.schedule(Time.inMilliseconds(this.T-((Math.random()-0.5)*this.T))); //1e3
    }

    /**
	 * This is the passive thread of the system. It defines the appropriate
     * reactions to the received <code>MessageType</code> of messages.
     * The <code>MessageType</code> defines 2 types of messages for the Peer
     * Sampling Service: The ACTION and REACTION.
     *
     * @param swapMessage the swap message that the passive state of Peer
     * Sampling Service can process
     *
	 */
    private void runPassiveState(SwapMessage swapMessage){
        switch(swapMessage.type){
            case REACTION:
                this.reactionsReceived=this.reactionsReceived+1.0;
                if(viewPropagationPolicy==viewPropagationPolicy.PUSHPULL){
                    this.viewManager.select(swapMessage.buffer);
                }
                else{
                    // do nothing
                }
                break;
            case ACTION:
                this.actionsReceived=this.actionsReceived+1.0;
                if(viewPropagationPolicy==viewPropagationPolicy.PUSHPULL){
                    this.sendBuffer(MessageType.REACTION, swapMessage.getSourceAddress());
                    this.reactionsSent=this.reactionsSent+1.0;
                    this.viewManager.select(swapMessage.buffer);
                }
                else{
                    // do nothing
                }
                break;
            default:
                // another message type has been received and just ignore it...
        }
        viewManager.increaseAge(A);
    }

    /**
	 * The common operations that are executed between the active and the
     *  passive thread. NOTE: Am i syncronizing correctly?
     *
     * 1. puts the local finger in the buffer
     * 2. permutes the view
     * 3. moves H oldest items at the end of the view
     * 4. appends c/2-1 items from the head of the view to the buffer
     * 5. sends the buffer to the destination
     *
     * @param messType the type of message to be sent (ACTIONS or REACTION)
     * @param destination the destination peer
	 */
    private void sendBuffer(MessageType messType, NetworkAddress destination){
        SwapMessage message=new SwapMessage();
        message.type=messType;
        message.buffer=new ArrayList();
        message.buffer.add(this.viewManager.getMyDescriptor().clone());
        synchronized(this.viewManager.getView()){
            this.viewManager.permute();
            this.viewManager.moveOldItemsAtTheEnd();
            message.buffer.addAll(this.viewManager.getSomeNeighbors());
        }
        this.getPeer().sendMessage(destination, message);
        parosMess+=1.0;
    }

    /**
	 * Gurantees the handle of a <code>SwapMessage</code> message.
     *
     * @param message the received message in the peer
	 */
    @Override
    public void handleIncomingMessage(Message message){
        if(message instanceof SwapMessage){
            SwapMessage swapMessage=(SwapMessage) message;
            this.runPassiveState(swapMessage);
        }
    }

    /**
	 * Scedules the measurements to be collected at the end of every epoch. It
     * also resets the state of the metrics retained during runtime
	 */
    private void scheduleMeasurements(){
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener()
        {
        	@Override
			public String getId() 
			{
				return "PeerSamplingService";
			}
        	
            public void measurementEpochEnded(MeasurementLog log, int epochNumber)
            {
//                for(FingerDescriptor neighbor:viewManager.getView()){
//                    getPeer().getMeasurementLogger().log(neighbor.getNetworkAddress(), 1);
//                }
//                getPeer().getMeasurementLogger().log(PSSMeasurementTags.MESSAGE_ACTION_RECEIVED, actionsReceived);
//                getPeer().getMeasurementLogger().log(PSSMeasurementTags.MESSAGE_REACTION_REVEIVED, reactionsReceived);
//                getPeer().getMeasurementLogger().log(PSSMeasurementTags.MESSAGE_ACTION_SENT, actionsSent);
//                getPeer().getMeasurementLogger().log(PSSMeasurementTags.MESSAGE_REACTION_SENT, actionsSent);
//                actionsReceived=0.0;
//                reactionsReceived=0.0;
//                actionsSent=0.0;
//                reactionsSent=0.0;
                log.log(epochNumber, PSSMeasurementTags.PAROS_MESS, parosMess);
                parosMess=0.0;
                
                // shrinking - add eag 2017-05-02
                final int 	epoch_min = log.getMinEpochNumber(),
    					kl = log.getMaxEpochNumber(),
    					kf = Math.max(kl - 100 , epoch_min );
                
                //System.out.printf( "PeerSamplingService: log shrink %d -> %d\n", kf, kl );
                log.shrink(kf, kl);
                
                
            }

			
        });
    }

}
