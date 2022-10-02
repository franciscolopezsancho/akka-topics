package faulttolerance3.exception

//FileWatcherCapabilities exception
@SerialVersionUID(1L)
class ClosedWatchServiceException(msg: String)
    extends Exception(msg)
    with Serializable
