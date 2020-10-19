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
	private int windowSize;
	private int windowState; // free places on the window
	private int firstWindowPacket; // fist packet of the window
	private int lastWindowPacket;

	private State state;

	public FT20ClientGBN() {
		super(true, "FT20-ClientGBN");
	}

	public int initialise(int now, int node_id, Node nodeObj, String[] args) {
		super.initialise(now, node_id, nodeObj, args, this);

		raf = null;
		file = new File(args[0]);
		BlockSize = Integer.parseInt(args[1]);
		windowSize = Integer.parseInt(args[2]);
		windowState = 0;
		firstWindowPacket = 0;
		nextPacketSeqN = 1;

		state = State.BEGINNING;
		lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);

		sendNextPacket(now);
		return 1;
	}

	@Override
	public void on_clock_tick(int now) {
		// super.on_clock_tick(now);
		if (nextPacketSeqN <= firstWindowPacket + windowSize - windowState && windowState > 0
				&& (nextPacketSeqN <= lastPacketSeqN || state == State.FINISHING)) {
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
				windowState--;
				break;
			case FINISHING:
				super.sendPacket(now, SERVER, new FT20_FinPacket(nextPacketSeqN, now));
				windowState = 0;
				break;
		}
		self.set_timeout(DEFAULT_TIMEOUT);

	}

	public void on_timeout(int now) {
		super.on_timeout(now);
		// send complete window from beginning
		nextPacketSeqN = firstWindowPacket;
		windowState = windowSize;
	}

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
		switch (state) {
			case BEGINNING:
				state = State.UPLOADING;
				windowState = windowSize - 1;
			case UPLOADING:
				firstWindowPacket = ack.cSeqN + 1;
				/* nextPacketSeqN >= firstWindowPacket + windowSize - windowState || */
				if (nextPacketSeqN < firstWindowPacket)
					nextPacketSeqN = firstWindowPacket;
				if (nextPacketSeqN > lastPacketSeqN)
					state = State.FINISHING;
				// TODO confirmar se nao da casos a menos
				if (windowState < windowSize)
					windowState++; // more free space on the window
				break;
			case FINISHING:
				super.log(now, "All Done. Transfer complete...");
				super.printReport(now);
				return;
		}
		// sendNextPacket(now);

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