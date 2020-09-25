import java.util.Arrays;
import cnss.simulator.*;
import cnss.lib.*;
import java.nio.ByteBuffer;

public class SenderNode extends AbstractApplicationAlgorithm {

  public SenderNode() {
      super(true, "sender-node");
  }

  public int initialise(int now, int node_id, Node self, String[] args) {
    super.initialise(now, node_id, self, args);
    //self.set_timeout(5000);
    return 5000;
	}

  public void on_clock_tick(int now) {
      ByteBuffer msg = ByteBuffer.allocate(10);
      msg.putInt(now);
      //byte[] msg = bigIntToByteArray(now);

      self.send( self.createDataPacket( 1, msg.array()));
      
    }
  public void on_receive( int now, DataPacket p ) {
    int start = ByteBuffer.wrap(p.getPayload()).getInt();
    int rtt = now - start;
    log( now, "took: " + rtt); 
    
  }

  /*private byte[] bigIntToByteArray(int i ) {
    BigInteger bigInt = BigInteger.valueOf(i);      
    return bigInt.toByteArray();
}*/
} 