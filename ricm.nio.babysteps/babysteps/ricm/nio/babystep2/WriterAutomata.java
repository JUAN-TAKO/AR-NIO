package ricm.nio.babystep2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import ricm.nio.babystep2.ReaderAutomata.State;

public class WriterAutomata {
	enum State {WRITING_LENGTH, WRITING_MSG, WRITING_IDLE} ;
	
	State state = State.WRITING_IDLE;
	byte[] data;
	ByteBuffer buffer = ByteBuffer.allocate(64);
	Queue<byte[]> queue = new LinkedList<byte[]>();
	int length;
	int to_write;
	SocketChannel sc;
	
	public WriterAutomata(SelectionKey key) {
		sc = (SocketChannel) key.channel();
	}
	
	public void sendMsg(byte[] msg) throws IOException {
		if (state == State.WRITING_IDLE) state = State.WRITING_LENGTH;
		queue.add(msg);
	}
	
	void handleWrite() throws IOException{
		
		if (state == State.WRITING_IDLE) {
			if (!queue.isEmpty()) state = State.WRITING_LENGTH;
		}
		
		else if (state == State.WRITING_LENGTH) {
			byte[] msg = queue.peek();
			length = msg.length;
			to_write = length;
			buffer.putInt(length);
			state = State.WRITING_MSG;
		} 
		
		else if (state == State.WRITING_MSG) {
			byte[] msg = queue.peek();
			int r = Math.min(msg.length, buffer.al);
			buffer.put(message, 0, r);
			to_write -= r;
			if(to_write == 0)
				state = State.WRITING_LENGTH;
		}
	}
}
