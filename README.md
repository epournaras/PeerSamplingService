# The Peer Sampling Service

This project is the source code that prototypes the Peer Sampling Service DIAS as illustrated in the following published [paper](http://www.casmodeling.com/content/pdf/2194-3206-1-19.pdf):

>M. Jelasity, S. Voulgaris, R. Guerraoui, A.M. Kermarrec and M. van Steen, _Gossip-based peer sampling_, ACM Transaction on Computer Systems, 25, 3, Article 8, August 2007.

This implementation is used extensively in the following [PhD thesis](http://evangelospournaras.com/wordpress/wp-content/uploads/2013/04/Multi-level-Reconfigurable-Self-organization-in-Overlay-Services1.pdf).

>E. Pournaras, _Multi-level Reconfigurable Self-organization in Overlay Services_, PhD Thesis, Delft University of Technology, March 2013

Summary
---

Gossip-based communication protocols are appealing in large-scale distributed applications such as information dissemination, aggregation, and overlay topology management. This paper factors out a fundamental mechanism at the heart of all these protocols: the peer-sampling service. In short, this service provides every node with peers to gossip with. We promote this service to the level of a first-class abstraction of a large-scale distributed system, similar to a name service being a first-class abstraction of a local-area system. We present a generic framework to implement a peer-sampling service in a decentralized manner by constructing and maintaining dynamic unstructured overlays through gossiping membership information itself. Our framework generalizes existing approaches and makes it easy to discover new ones. We use this framework to empirically explore and compare several implementations of the peer-sampling service. Through extensive simulation experiments we show that---although all protocols provide a good quality uniform random stream of peers to each node locally---traditional theoretical assumptions about the randomness of the unstructured overlays as a whole do not hold in any of the instances. We also show that different design decisions result in severe differences from the point of view of two crucial aspects: load balancing and fault tolerance. Our simulations are validated by means of a wide-area implementation.
