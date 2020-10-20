import java.io.*;

import ft20.*;
import cnss.simulator.*;

public class FT20ClientGBN extends FT20AbstractApplication implements FT20_PacketHandler {

	static int SERVER = 1;

	enum State {
		BEGINNING, UPLOADING, FINISHING
	};

	static int DEFAULT_TIMEOUT = 1000;

	private File file;
	private RandomAccessFile raf;
	private int BlockSize;
	private int nextPacketSeqN, lastPacketSeqN;
	private int firstPacketWindow, lastPacketWindow, windowSize;

	private State state;

	public FT20ClientGBN() {
		super(true, "FT20-ClienteGBN");
	}

	public int initialise(int now, int node_id, Node nodeObj, String[] args) {
		super.initialise(now, node_id, nodeObj, args, this);

		raf = null;
		file = new File(args[0]);
		BlockSize = Integer.parseInt(args[1]);
		windowSize = Integer.parseInt(args[2]);
		nextPacketSeqN = 0;

		state = State.BEGINNING;
		lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);
		sendNextPacket(now);
		return 1;
	}

	public void on_clock_tick(int now) {
		if ((nextPacketSeqN < lastPacketWindow && nextPacketSeqN >= firstPacketWindow)
				&& nextPacketSeqN <= lastPacketSeqN) {
			sendNextPacket(now);
		}

	}

	private void sendNextPacket(int now) {
		switch (state) {
			case BEGINNING:
				super.sendPacket(now, SERVER, new FT20_UploadPacket(file.getName(), now));
				break;
			case UPLOADING:
				super.sendPacket(now, SERVER, readDataPacket(file, nextPacketSeqN, now));
				nextPacketSeqN++;
				break;
			case FINISHING:
				super.sendPacket(now, SERVER, new FT20_FinPacket(nextPacketSeqN, now));
				break;
		}

		self.set_timeout(DEFAULT_TIMEOUT);
		

	}

	@Override
	public void on_timeout(int now) {
		super.on_timeout(now);
		nextPacketSeqN = firstPacketWindow;
		sendNextPacket(now);

	}

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
		switch (state) {
			case BEGINNING:
				state = State.UPLOADING;
				nextPacketSeqN = 1;
			case UPLOADING:
				firstPacketWindow = ack.cSeqN + 1;
				lastPacketWindow = firstPacketWindow + windowSize;

				if (firstPacketWindow > lastPacketSeqN) {
					state = State.FINISHING;
					sendNextPacket(now);
				}
				break;
			case FINISHING:
				if(ack.cSeqN == lastPacketSeqN +1) {
					super.log(now, "All Done. Transfer complete...");
					super.printReport(now);
				}
				return;
		}
		self.set_timeout(DEFAULT_TIMEOUT);
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