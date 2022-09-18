## checking cluster status through akka-management-cluster-http

Once you have deployed the application as shown in the chapter you can tie local calls to the port 8558 to any of the running pods. Execute the following to find the name of the pods. 
                                                                    
	kubectl -n akka-cluster get po

You should get something of this sort.

	NAME                                    READY   STATUS    RESTARTS   AGE
	testing-bootstrap13b-79995bbd5d-5n75t   1/1     Running   0          5m4s
	testing-bootstrap13b-79995bbd5d-bdhll   1/1     Running   0          5m4s
	testing-bootstrap13b-79995bbd5d-g8f9x   1/1     Running   0          5m4s


Now to link your local port with one of the pods enter the following.
	
	kubectl port-forward testing-bootstrap13b-79995bbd5d-5n75t 8558:8558

The output of this command should be something like this. 
	
	Forwarding from 127.0.0.1:8558 -> 8558
	Forwarding from [::1]:8558 -> 8558

Now you can go to your browser - Firefox formats the output - and enter the following. 

	http://localhost:8558/cluster/members 