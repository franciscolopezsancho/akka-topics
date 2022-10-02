package faulttolerance3.exception

//DbWriter's connector exceptions
@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String)
    extends Exception(msg)
    with Serializable
