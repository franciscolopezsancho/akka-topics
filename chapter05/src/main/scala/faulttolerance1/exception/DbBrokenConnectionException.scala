package faulttolerance1.exception

@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String)
    extends Exception(msg)
    with Serializable
