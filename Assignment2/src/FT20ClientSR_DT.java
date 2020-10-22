import java.io.*;

import ft20.*;
import cnss.simulator.*;

public class FT20ClientSR_DT extends FT20AbstractApplication implements FT20_PacketHandler {

	private static final double VARIANCE_WEIGHT = 4.0;
	private static final double BETA = 0.25;
	private static final double ALPHA = 0.125;
	static int SERVER = 1;

	enum State {
		BEGINNING, UPLOADING, FINISHING
	};

	static int DEFAULT_TIMEOUT = 100;
	private static final int RECEIVED_ACK = -1;

	private File file;
	private RandomAccessFile raf;
	private int BlockSize;
	private int nextPacketSeqN, lastPacketSeqN;
	private int firstPacketWindow, lastPacketWindow; // fist and last packets on the window
	private int windowSize;
	private int[] window;
	private int[] time;
	private int timeOut;
	private double rtt;
	private double v;

	private State state;

	public FT20ClientSR_DT() {
		super(true, "FT20-ClienteSR-DT");
	}

	public int initialise(int now, int node_id, Node nodeObj, String[] args) {
		super.initialise(now, node_id, nodeObj, args, this);

		raf = null;
		file = new File(args[0]);
		BlockSize = Integer.parseInt(args[1]);
		windowSize = Integer.parseInt(args[2]);
		nextPacketSeqN = 0;
		window = new int[windowSize];
		rtt = now;
		v = 0;

		time = new int[windowSize];
		timeOut = DEFAULT_TIMEOUT;

		for (int i = 0; i < windowSize; i++) {
			window[i] = RECEIVED_ACK;
			time[i] = now;
		}

		state = State.BEGINNING;
		lastPacketSeqN = (int) Math.ceil(file.length() / (double) BlockSize);
		sendNextPacket(now);
		return 1;
	}

	public void on_clock_tick(int now) {

		int i = 0;
		while (i < windowSize && state == State.UPLOADING) {
			if (window[i] > 0) { // decrease timeout
				window[i] = window[i] - 1;
			} else if (window[i] == 0) { // the timeout is over
				on_timeout(now, i);
				break;
			}
			i++;
		}

		// if a timeout did not occurred and the next packet to send can fit in the
		// window
		if (i >= windowSize && (nextPacketSeqN < lastPacketWindow && nextPacketSeqN >= firstPacketWindow)
				&& nextPacketSeqN <= lastPacketSeqN) {
			window[nextPacketSeqN - firstPacketWindow] = timeOut;
			sendNextPacket(now);
		}

	}

	private void sendNextPacket(int now) {
		switch (state) {
			case BEGINNING:
				rtt = now;
				super.sendPacket(now, SERVER, new FT20_UploadPacket(file.getName(), now));
				self.set_timeout(timeOut);
				break;
			case UPLOADING:
				super.sendPacket(now, SERVER, readDataPacket(file, nextPacketSeqN, now));
				time[nextPacketSeqN - firstPacketWindow] = now;
				nextPacketSeqN++;// increment the next packet to send
				break;
			case FINISHING:
				super.sendPacket(now, SERVER, new FT20_FinPacket(nextPacketSeqN, now));
				self.set_timeout(timeOut);
				break;
		}

	}

	@Override
	public void on_timeout(int now) {
		// timeout to the UPLOAD and FIN packets
		super.on_timeout(now);
		sendNextPacket(now);
	}

	public void on_timeout(int now, int p) {
		super.on_timeout(now);
		super.sendPacket(now, SERVER, readDataPacket(file, firstPacketWindow + p, now));
		time[p] = now;
		window[p] = timeOut; // arm new timeout

	}

	@Override
	public void on_receive_ack(int now, int client, FT20_AckPacket ack) {
		switch (state) {
			case BEGINNING:
				state = State.UPLOADING;
				nextPacketSeqN = 1;
				firstPacketWindow = ack.cSeqN + 1;
				lastPacketWindow = firstPacketWindow + windowSize;
				break;

			case UPLOADING:
				int ackNum = ack.sSeqN - firstPacketWindow;
				int slide = ack.cSeqN + 1 - firstPacketWindow;
				if (ack.sSeqN == 1) { // if ack of the first packet then define adaptive timeout.
					rtt = now - time[ackNum];
				}

				if (ackNum >= 0) { // se não for um ack antigo
					window[ackNum] = RECEIVED_ACK;

					int sampleRTT = now - time[ackNum];
					v = computeVarianceRTT(sampleRTT);
					rtt = computeMeanRTT(sampleRTT);

					timeOut = (int) (rtt + VARIANCE_WEIGHT*v);
					super.tallyRTT((int) rtt);
					super.tallyTimeout(timeOut);
					moveWindow(ackNum, slide);
				} else if (slide > 0) {
					// move the window to the next packet from the cumulative ack
					moveWindow(ackNum, slide);
				}
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

	}

	private double computeMeanRTT(int sampleRTT) {
		return (rtt * (1-ALPHA) + sampleRTT * ALPHA);
	}

	private double computeVarianceRTT(int sampleRTT) {
		return (1-BETA) * v + Math.abs(sampleRTT - rtt) * BETA;
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
			for (int i = 0; i < windowSize - slide; i++) {// 0
				window[i] = window[i + slide];
				time[i] = time[i + slide];
			}
			for (int i = windowSize - slide; i < windowSize; i++) {
				window[i] = RECEIVED_ACK;
				time[i] = 0;

			}
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