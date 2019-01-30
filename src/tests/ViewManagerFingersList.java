package tests;

import java.util.ArrayList;
import java.util.List;

import components.AgeDescriptorManager;
import components.ViewManager;
import dsutil.protopeer.*;
import enums.PeerSelectionPolicy;
import protopeer.Finger;
import protopeer.PeerIdentifier;
import protopeer.RingIdentifier;
import protopeer.network.NetworkAddress;
import protopeer.network.zmq.ZMQAddress;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import protopeer.util.RandomnessSource;
import protopeer.MainConfiguration;

public class ViewManagerFingersList 
{

	
	public static void main(String[] args) 
	{
		// objective: test equals operator between a list of peers, following  a bug in PSS that shows duplicates in the PSS view
		// list is obtained from the pss table used for logging PSS views
		
		final String 	identifierList = "(tcp://127.0.0.1:52585, 0.5533818927597023)|(tcp://127.0.0.1:62926, 0.5533818927597023)|(tcp://127.0.0.1:51324, 0.5533818927597023)|(tcp://127.0.0.1:62926, 0.5533818927597023)|(tcp://127.0.0.1:50899, 0.5533818927597023)|(tcp://127.0.0.1:52585, 0.5533818927597023)|(tcp://127.0.0.1:61743, 0.5533818927597023)|(tcp://127.0.0.1:50022, 0.5533818927597023)|(tcp://127.0.0.1:61743, 0.5533818927597023)|(tcp://127.0.0.1:49406, 0.5533818927597023)|(tcp://127.0.0.1:50022, 0.5533818927597023)|(tcp://127.0.0.1:63516, 0.5533818927597023)|(tcp://127.0.0.1:51324, 0.5533818927597023)|(tcp://127.0.0.1:63516, 0.5533818927597023)|(tcp://127.0.0.1:52000, 0.5533818927597023)|(tcp://127.0.0.1:54277, 0.5533818927597023)|(tcp://127.0.0.1:52000, 0.5533818927597023)|(tcp://127.0.0.1:54277, 0.5533818927597023)|(tcp://127.0.0.1:59925, 0.5533818927597023)|(tcp://127.0.0.1:49406, 0.5533818927597023)|";
		
		final boolean 	create_duplicates = false;
		
		System.out.println( "#create_duplicates : " + create_duplicates);
		
		ArrayList<FingerDescriptor> 	finger_descriptors = new ArrayList<FingerDescriptor>();
		
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 52585), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 62926), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 51324), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 62926), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 50899), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates ) 
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 52585), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 61743), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 50022), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 61743), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 49406), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 50022), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 63516), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 51324), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 63516), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 52000), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 54277), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 52000), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 54277), new RingIdentifier(0.5533818927597023) )));
		finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 59925), new RingIdentifier(0.5533818927597023) )));
		if( create_duplicates )
			finger_descriptors.add( new FingerDescriptor(new Finger(new ZMQAddress("127.0.0.1", 49406), new RingIdentifier(0.5533818927597023) )));
		
		System.out.println( "#finger_descriptors : " + finger_descriptors.size() );
		
		// set age descriptor, required by ViewManager
		AgeDescriptorManager	ageDescriptorManager = new AgeDescriptorManager( );
		
		for( FingerDescriptor descriptor : finger_descriptors)
		{
			ageDescriptorManager.initAge(descriptor);
			ageDescriptorManager.setAge(descriptor,2.0);
		}
		
		
		// create a view manager
		// DIAS defaults
		final int 			c = 50,
							H = 1,
							S = 24;
		
		Finger 				myFinger = new Finger(new ZMQAddress("127.0.0.1", 9000), new RingIdentifier(0.5533818927597023));
		
		ViewManager			vw = new ViewManager(c, H, S, PeerSelectionPolicy.RAND, myFinger);
		
		// add to view manager
		vw.select(finger_descriptors);
		
		// get info from view manager
		final	boolean		has_duplicates = vw.hasDuplicatesInView();		
		System.out.println("has_duplicates : " + has_duplicates);
		
		final int			view_size = vw.getView().size();
		System.out.println( "view size : " + view_size );
			 
	}// main
}// class
