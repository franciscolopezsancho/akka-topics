// package words

// import akka.actor.typed.{ ActorSystem, Behavior }
// import akka.actor.typed.scaladsl.Behaviors

// object Main extends App {

//   val guardian = ActorSystem(Behaviors.empty, "words")
// }

// object Guardian {

//   def apply(): Behavior[Unit] = Behaviors.setup { context => 
//   	val cluster = Cluster(context.self)

//   	if (cluster.member.hasRole("master")){
//   		val master = spawn(Master,"master-0")
//   	}
//   	if cluster.member.hasRole("worker"){
//   		///look for properties 
//   		val numWorkers = context.system.setting.config.getInt("mapreduce.worker-per-node") 
//   		for (i <- 0 to numWorkers) {
//   		val worker = spawn(Worker(), s"worker-$i")
//   		}
//   	}	
//   }

//     Behaviors.empty
// }

// object Master {
// 	def apply() = Behaviors.empty
// }

// object Worker {
// 	def apply() = Behaviors.empty
// }


