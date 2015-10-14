package lightning

import kafka.serializer.Encoder

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 10/14/15
 * Time: 11:45 AM
 *
 */
class PlayJsonEncoder extends Encoder[Object] {


  def toBytes(t: Object) = ???
}
