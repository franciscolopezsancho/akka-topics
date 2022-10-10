## Known issues

When starting the second node 

	sbt "chapter13a/runMain example.clustering2.App 2" 


you may get the following error

	akka.remote.RemoteTransportException: Failed to bind TCP to [127.0.0.2:25520] due to: Bind failed because of java.net.BindException: [/127.0.0.2:25520] Can't assign requested address

This happens in MacOs because loopback addresses are all disabled except for 127.0.0.1. To enable them enter the following.

	sudo ifconfig lo0 alias 127.0.0.2 up 