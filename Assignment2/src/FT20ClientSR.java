import java.io.*;

import ft20.*;
import cnss.simulator.*;

public class FT20ClientSR extends FT20AbstractApplication implements FT20_PacketHandler {

	static int SERVER = 1;

	enum State {
		BEGINNING, UPLOADING, FINISHING
	};

	static int DEFAULT_TIMEOUT = 1000;
	private static final int RECEIVED_ACK = -1;
	private static final int NON_RECEIVED_ACK = 0;
	

	private File file;
	private RandomAccessFile raf;
	private int BlockSize;
	private int nextPacketSeqN, lastPacketSeqN;
	private int firstPacketWindow, lastPacketWindow; // fist and last packets on the window
	private int windowSize, timeout;
	private int[] window;
	// private int counter;

	private State state;

	public FT20ClientSR() {
		super(true, "FT20-ClienteGBN");
	}

	public int initialise(int now, int node_id, Node nodeObj, String[] args) {
		super.initialise(now, node_id, nodeObj, args, this);

		raf = null;
		file = new File(args[0]);
		BlockSize = Integer.parseInt(args[1]);
		windowSize = Integer.parseInt(args[2]);
		nextPacketSeqN = 0;
		window = new int[windowSize]; 
		timeout = -1;
		

		state = State.BEGINNING;
		lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);
		sendNextPacket(now);
		return 1;
	}

	public void on_clock_tick(int now) {
		/**
		 * If the next packet do send can fit in the window we send it (if does not pass
		 * the last one)
		 */
		/*
		 * if (counter >= 0) { runSend(now);
		 * 
		 * } else
		 */if ((nextPacketSeqN < lastPacketWindow && nextPacketSeqN >= firstPacketWindow)
				&& nextPacketSeqN <= lastPacketSeqN) {
			sendNextPacket(now);
		}
		if (timeout > 0) { // decrease timeout
			timeout--;
		}

	}

	private void sendNextPacket(int now) {
		switch (state) {
			case BEGINNING:
				super.sendPacket(now, SERVER, new FT20_UploadPacket(file.getName(), now));
				break;
			case UPLOADING:
				super.sendPacket(now, SERVER, readDataPacket(file, nextPacketSeqN, now));
				// increment the next packet to send
				nextPacketSeqN++;
				break;
			case FINISHING:
				super.sendPacket(now, SERVER, new FT20_FinPacket(nextPacketSeqN, now));
				break;
		}
		if (timeout <= 0) { // no timeout setted yet
			timeout = DEFAULT_TIMEOUT;
		}

		self.set_timeout(timeout);
	}

	@Override
	public void on_timeout(int now) {
		super.on_timeout(now);
		// nextPacketSeqN = firstPacketWindow;

		if (state != State.UPLOADING) {
			sendNextPacket(now);
		} else {
			if (window[0] != RECEIVED_ACK) {
				super.sendPacket(now, SERVER, readDataPacket(file, firstPacketWindow, now));
			}
			timeout = DEFAULT_TIMEOUT;
			self.set_timeout(timeout);
			// runSend(now);
		}
	}

	/**
	 * Resets the timeout if the ack received is the expected ack. Puts the timeout
	 * to de DEFAULT value
	 * 
	 * @param ack
	 */
	private void resetTimeOut(FT20_AckPacket ack) {
		if (firstPacketWindow == ack.cSeqN) {
			timeout = DEFAULT_TIMEOUT;
		}
	}

	/*
	 * public void runSend(int now) { if (counter >= 0 && counter < windowSize) { if
	 * (window[counter++] != RECEIVED_ACK) { super.sendPacket(now, SERVER,
	 * readDataPacket(file, firstPacketWindow + counter, now));
	 * self.set_timeout(DEFAULT_TIMEOUT); } else { runSend(now); } } if (counter >=
	 * windowSize) counter = -1; }
	 */

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
		switch (state) {
			case BEGINNING:
				state = State.UPLOADING;
				nextPacketSeqN = 1;
				firstPacketWindow = ack.cSeqN + 1;
				lastPacketWindow = firstPacketWindow + windowSize;
				resetTimeOut(ack);
				break;

			case UPLOADING:
				// move the window to the next packet from the cumulative ack
				// firstPacketWindow = ack.cSeqN + 1;
				resetTimeOut(ack);
				int ackNum = ack.sSeqN - firstPacketWindow; // 0
				int slide = ack.cSeqN + 1 - firstPacketWindow; // 10

				moveWindow(ackNum, slide);
				/**
				 * If the beginning of the window is bigger then the last packet to send then
				 * the sending is done. Send the final packet
				 */
				if (firstPacketWindow > lastPacketSeqN) {
					state = State.FINISHING;
					sendNextPacket(now);
				}
				break;
			case FINISHING:
				/**
				 * print the statistics of the server only when the ack of the FIN packet is
				 * received
				 */
				if (ack.cSeqN == lastPacketSeqN + 1) {
					super.log(now, "All Done. Transfer complete...");
					super.printReport(now);
				}
				return;
		}
		// rearm timeout after receiving a ack
		self.set_timeout(timeout);
	}

	/**
	 * Moves the window if necessary (if the cumulative ack+1 is greater then the
	 * first packet of the window). The window will move the beguin to the
	 * cumulative ack+1, and te new free espace will be marked as non-received ack.
	 * The values os the received acks will be pushed to the correspondent position
	 * 
	 * @param ackNum position in the window vector of the corresponding ack
	 * @param slide  number of positions to move the window
	 */
	private void moveWindow(int ackNum, int slide) {
		if (slide > 0) {
			for (int i = 0; i < windowSize - slide; i++) // 0
				window[i] = window[i + slide];
			for (int i = windowSize - slide; i < windowSize; i++)
				window[i] = NON_RECEIVED_ACK;
			firstPacketWindow += slide;
			lastPacketWindow = firstPacketWindow + windowSize;

		} else {
			window[ackNum] = RECEIVED_ACK;
		}
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