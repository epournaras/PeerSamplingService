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

package components;

import java.util.ArrayList;
import java.util.List;
import enums.PeerSelectionPolicy;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import protopeer.Finger;
import dsutil.protopeer.FingerDescriptor;


/**
 * Defines a set of operations performed over a list of neighbor descriptors. 
 * The order of the descriptors in the list remains unchnaged except when 
 * methods such as <code>permute()</code> change the order of the elements. This
 * class replaces the role of <code>NeighborManager</code>.
 *
 * @author Evangelos
 */
public class ViewManager{

    private Queue<FingerDescriptor> samples;
    private final List<FingerDescriptor> view;
    private final FingerDescriptor myDescriptor;
    private int c;
    private int H;
    private int S;
    private PeerSelectionPolicy policy;
    private AgeDescriptorManager ageManager;
    
    /**
	 * Requires a set of parameters in order the defined operations to be
     * applied on the view. 
     *
     * @param c the view length
     * @param H the healing parameter
     * @param S the swap parameter
     * @param peerSelectionPolicy the peer selection police. It can by RAND or
     * OLD
     * @param myFinger the finger of the local peer. It is used to construct the
     * local descriptor
	 */
    public ViewManager (int c, int H, int S, PeerSelectionPolicy peerSelectionPolicy, Finger myFinger){
        this.c=c;
        this.H=H;
        this.S=S;
        this.policy=peerSelectionPolicy;
        
        // mod edward + renato | 2019-01-23
        //this.view=new ArrayList<FingerDescriptor>();
        this.view= Collections.synchronizedList( new ArrayList<FingerDescriptor>());
        this.samples=new ConcurrentLinkedQueue<FingerDescriptor>();
        this.ageManager=new AgeDescriptorManager();
        this.myDescriptor=new FingerDescriptor(myFinger);
        this.ageManager.initAge(myDescriptor);
    }

    /**
	 * Initializes the view with the initial bootstrap neighbors
     *
     * @param bootstrapPeers the neighbors that <code>NeighborManager</code> holds
	 */
    public void setBootstrapPeers(Collection<Finger> bootstrapPeers){
        synchronized(this.view){
            for(Finger peer:bootstrapPeers){
                FingerDescriptor descriptor=new FingerDescriptor(peer.clone());
                this.ageManager.initAge(descriptor);
                this.getView().add(descriptor);
            }
            //gurantees there are no more than c nodes in the view at the beginning
            this.select(new ArrayList());
        }
    }

    /**
	 * Updates the sampels in the queue after an update, that is the call of the
     * select() method. First the old elements are removed from the view and
     * then the new ones are added in the queue.
     *
	 */
    private void updateSamples(){
        //step 1: removes from the queue items do not exist in the view anymore
        for(FingerDescriptor descriptor:this.getSamples()){
            if(!this.view.contains(descriptor)){
                this.getSamples().remove(descriptor);
            }
        }
        //step 2: add new items in the queue
        for(FingerDescriptor descriptor:this.view){
            if(!this.samples.contains(descriptor)){
                this.getSamples().add(descriptor);
            }
        }
    }


    /**
	 * Increases the age of the neighbor descriptors
     *
     * @param timePassed the time passed as it it defined by the used timers
	 */
    public void increaseAge(double timePassed){
        synchronized(this.view){
            for(FingerDescriptor neighbor:this.getView()){
                this.ageManager.increaseAge(neighbor, timePassed);
            }
        }
    }

    /**
	 * Selects a peer from the view to gossip with. The selction can be done by
     * following two different the RAND and OLD policies. In the first case, a
     * random descriptor is selected to gossip with, whereas, in the OLD policy,
     * the descriptor with the oldest age is returned. Note that this police is
     * reffered to the paper as "OLD". Hoewever, it refers to the oldest
     * descriptor.
	 */
    public FingerDescriptor selectPeer(){
        if(view.size()>0){
            switch(policy){
                case RAND:
                    return this.getView().get((int)(Math.random()*this.getView().size()));
                case OLD:
                    return this.ageManager.getOldestDescriptor(this.view);
                default:
                    return null; //no defined policy
            }
        }
        return null;
    }

    /**
	 * It performs a random permutation in the view. This is done before
     * moving the oldest H items to the end of the view. It gurantees that
     * together with the H oldest items the rest will be random. Algorithm found
     * in http://www.algoblog.com/2007/06/04/permutation/ and is the following:
     *
     * Algorithm for mermutation:
     * For i = 1 to n-1:
     *  Let j = r(i+1)
     *  Swap a[i] and a[j]
     *
     * The methods has been tested and works fine.
	 */
    public void permute(){
        for(int i=1;i<getView().size();i++){
            int j=(int)(Math.random()*(i+1));
            FingerDescriptor aj=getView().get(j);
            FingerDescriptor ai=getView().get(i);
            getView().set(i, aj);
            getView().set(j, ai);
        }
    }
    
    // hasDuplicatesInView: check if the view contains duplicate peers; should never be the case
    // add edward 2019-01-14
    public boolean hasDuplicatesInView()
    {
    	synchronized(this.view)
    	{
	        for(int i=0;i < getView().size();i++)
	        {                         
	        	for(int j=(i+1);j < getView().size();j++) 
	        	{
	        		if( getView().get(i).equals( getView().get(j)) )
	        			return true;
	        	}
	        }
	        
	        return false;
    	}
        
    }

    /**
	 * Returns c/2-1 neighbors from the head of the view. These neighbors are
     * added in the buffer sent together with a fresh descriptor of the local
     * peer.
	 */
    public Collection<FingerDescriptor> getSomeNeighbors(){
        Collection neighbors=new ArrayList();
        for(int i=0;i<Math.floor(this.getView().size()/2)-1;i++){
            FingerDescriptor finger=this.getView().get(i).clone();
            neighbors.add(finger);

        }
        return neighbors;
    }

    /**
	 * Creates the new view after the swap. Guarantees that the appropriate
     * neighbors are retained in the view and its size is c. The queue with the
     * samples is updated.
     *
     * @param buffer the received buffer of neighbors
	 */
    public void select(List<FingerDescriptor> buffer){
        synchronized(this.view){
            this.appendUnique(buffer);
            this.removeOldItems(Math.min(H, this.getView().size()-c));
            this.removeHead(Math.min(S, this.getView().size()-c));
            this.removeAtRandom(this.getView().size()-c);
            this.updateSamples();
        }
    }

    /**
	 * Merges the two buffers without inserting duplicates. If there is a
     * duplicate the most recent one is retained by keeping the order in the
     * list. It also gurantees that there are no any references (fingers)
     * of a peer to itself.
     *
     * @param buffer the received list with the neighbors
	 */
    private void appendUnique(List<FingerDescriptor> buffer){
        for(FingerDescriptor neighbor:buffer){
            if(!neighbor.equals(this.myDescriptor)){
                if(!this.view.contains(neighbor)){
                    this.getView().add(neighbor);
                }
                else{
                    //Here we find the position of the existing neighbor
                    int index=0;
                    for(FingerDescriptor dublicate:this.view){
                        if(dublicate.equals(neighbor)){
                            break;
                        }
                        index++;
                    }
                    if(this.ageManager.isOlder(this.getView().get(index), neighbor)){
                        this.getView().remove(index);
                        this.getView().add(neighbor);
                    }
                }
            }
        }
    }

    /**
	 * Removes the H oldest items in the view. The methods retains the order of
     * the remaining items in the view. The algorithm is as follows:
     *
     * 1. Create a sorted list with the indices of the view
     * 2. Create a list and insert the H oldest items there
     * 3. Remove the items from the view by quering the 2nd list
     *
     * Algorithm needs optimization but it works fine and is tested
     *
     * @param H the number of oldest descriptors to be removed
	 */
    private void removeOldItems(int H){
        ArrayList<Integer> sortedIndices=new ArrayList<Integer>();
        for(int i=0;i<this.getView().size();i++){
            sortedIndices.add(i);
        }
        for(int i=1;i<sortedIndices.size();i++){
            FingerDescriptor descriptor=this.getView().get(sortedIndices.get(i));
            for(int j=0;j<i;j++){
                FingerDescriptor comparedDescriptor=this.getView().get(sortedIndices.get(j));
                if(this.ageManager.isOlder(descriptor, comparedDescriptor)){
                    sortedIndices.add(j, sortedIndices.remove(i));
                    break;
                }
            }
        }
        ArrayList<FingerDescriptor> kOldestItems=new ArrayList<FingerDescriptor>();
        for(int i=0;i<H;i++){
           kOldestItems.add(this.getView().get(sortedIndices.get(i)));
        }
        this.getView().removeAll(kOldestItems);
    }

    /**
	 * Moves the H oldest items at the end of the view. It retains the ordering
     * of the items that are moved at the end. The algorithm works in 3 phases:
     *
     * 1. Create and initial a list wih the indices of the neighbors in the view
     * 2. Sort the indices in that list
     * 3. Scan the indices and move oldest neighbors at the end appropriatelly,
     * by keeping the order. At the same time update the indices of the right
     * neighbors due to the move of the neighbor.
     *
     * @param H the number of oldest descriptors to be moved
	 */
    public void moveOldItemsAtTheEnd(){
        //initialization
        ArrayList<Integer> sortedIndices=new ArrayList<Integer>();
        for(int i=0;i<this.getView().size();i++){
            sortedIndices.add(i);
        }
        //sort the movedNeighborIndex list
        for(int i=1;i<sortedIndices.size();i++){
            FingerDescriptor descriptor=this.getView().get(sortedIndices.get(i));
            for(int j=0;j<i;j++){
                FingerDescriptor comparedDescriptor=this.getView().get(sortedIndices.get(j));
                if(this.ageManager.isOlder(descriptor, comparedDescriptor)){
                    sortedIndices.add(j, sortedIndices.remove(i));
                    break;
                }
            }
        }
        //if the size of the view is less than c, we gurantee to move a
        //proportinal number of old items to H at the end
        int movedItems=this.getView().size()*H/c;
        int scannedItems=movedItems;
        //move items at the end and update the sortedIndices list on the fly
        for(int i=0;i<movedItems;i++){
            int min=Integer.MAX_VALUE;
            int movedItemIndex=-1;
            int scannedItemIndex=0;
            for(int j=0;j<scannedItems;j++){
                int indexElement=sortedIndices.get(j).intValue();
                if(indexElement<min){
                    min=indexElement;
                    movedItemIndex=scannedItemIndex;
                }
            }
            sortedIndices.remove(movedItemIndex);
            scannedItems--;
            this.getView().add(this.getView().remove(min));
            for(int j=0;j<sortedIndices.size();j++){
                Integer indexToNeighbor=sortedIndices.get(j);
                if(indexToNeighbor.intValue()>min){
                    indexToNeighbor=indexToNeighbor.intValue()-1;
                    sortedIndices.set(j, indexToNeighbor);
                }
            }
        }
    }

    /**
	 * Removes S items from the head of the view. This controls the swap
     * parameter.
     *
     * @param S the number of S items to be removed
	 */
    private void removeHead(int S){
        for(int i=0;i<S;i++){
            this.getView().remove(0);
        }

    }

    /**
	 * Removes r items to make the size of the view again equals to c
     *
     * @param r the number of randomly removed peers
	 */
    private void removeAtRandom(int r){
        for(int i=0;i<r;i++){
            this.getView().remove((int)(Math.random()*this.getView().size()));
        }
    }

    /**
     * @return the view
     */
    public List<FingerDescriptor> getView() {
        return view;
    }

    /**
     * @return the myDescriptor
     */
    public FingerDescriptor getMyDescriptor() {
        return myDescriptor;
    }

    /**
     * @return the samples
     */
    public Queue<FingerDescriptor> getSamples() {
        return samples;
    }
}
