import java.io.*;

import ft20.*;
import cnss.simulator.*;

public class FT20ClientSR_DT extends FT20AbstractApplication implements FT20_PacketHandler {

	static int SERVER = 1;

	enum State {
		BEGINNING, UPLOADING, FINISHING
	};

	static int DEFAULT_TIMEOUT = 1000;

	private File file;
	private RandomAccessFile raf;
	private int BlockSize;
	private int nextPacketSeqN, lastPacketSeqN;

	private State state;

	public FT20ClientSR_DT() {
		super(true, "FT20-ClienteSR_DT");
	}

	public int initialise(int now, int node_id, Node nodeObj, String[] args) {
		super.initialise(now, node_id, nodeObj, args, this);

		raf = null;
		file = new File(args[0]);
		BlockSize = Integer.parseInt(args[1]);

		state = State.BEGINNING;
		lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);

		sendNextPacket(now);
		return 0;
	}

	private void sendNextPacket(int now) {
		switch (state) {
			case BEGINNING:
				super.sendPacket(now, SERVER, new FT20_UploadPacket(file.getName(), now));
				break;
			case UPLOADING:
				super.sendPacket(now, SERVER, readDataPacket(file, nextPacketSeqN, now));
				break;
			case FINISHING:
				super.sendPacket(now, SERVER, new FT20_FinPacket(nextPacketSeqN, now));
				break;
		}
		self.set_timeout(DEFAULT_TIMEOUT);
	}

	public void on_timeout(int now) {
		super.on_timeout(now);
		sendNextPacket(now);
	}

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
		switch (state) {
			case BEGINNING:
				state = State.UPLOADING;
			case UPLOADING:
				nextPacketSeqN = ack.cSeqN + 1;
				if (nextPacketSeqN > lastPacketSeqN)
					state = State.FINISHING;
				break;
			case FINISHING:
				super.log(now, "All Done. Transfer complete...");
				super.printReport(now);
				return;
		}
		sendNextPacket(now);
	}

	private FT20_DataPacket readDataPacket(File file, int seqN, int timestamp) {
		try {
			if (raf == null)
				raf = new RandomAccessFile(file, "r");

			raf.seek(BlockSize * (seqN - 1));
			byte[] data = new byte[BlockSize];
			int nbytes = raf.read(data);
			return new FT20_DataPacket(seqN, timestamp, data, nbytes);
		} catch (Exception x) {
			throw new Error("Fatal Error: " + x.getMessage());
		}
	}
}