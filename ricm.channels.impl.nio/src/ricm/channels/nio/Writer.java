package ricm.channels.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class Writer {

	enum State {WRITING_LENGTH, WRITING_MSG, WRITING_IDLE} ;
	
	State state = State.WRITING_IDLE;
	byte[] data;
	ByteBuffer buffer = ByteBuffer.allocate(64);
	Queue<byte[]> queue = new LinkedList<byte[]>();
	int length;
	int written;
	SelectionKey key;
	SocketChannel sc;
	
	public Writer(SelectionKey k) {
		key = k;
		sc = (SocketChannel) key.channel();
	}
	
	public void sendMsg(byte[] msg) throws IOException {
		if (state == State.WRITING_IDLE) state = State.WRITING_LENGTH;
		System.out.println("send_ " + new String(msg));
		queue.add(msg);
	}
	
	private boolean write() throws IOException {
		buffer.rewind();
		int n = sc.write(buffer);
		if (n == -1) {
			key.cancel();  // communication with server is broken
			sc.close();
			return false;
		}
		buffer.rewind();
		return true;
	}
	
	public boolean handleWrite() throws IOException{
		System.out.println("handle write");
		if (state == State.WRITING_IDLE) {
			System.out.println("IDLE");
			if (!queue.isEmpty()) state = State.WRITING_LENGTH;
		}
		
		else if (state == State.WRITING_LENGTH) {
			
			byte[] msg = queue.peek();
			length = msg.length;
			written = 0;
			buffer.putInt(length);
			System.out.println("writing length " + length);
		
			state = State.WRITING_MSG;
		} 
		
		else if (state == State.WRITING_MSG) {
			System.out.println("writing msg");
			byte[] msg = queue.peek();
			int w = Math.min(length - written, buffer.capacity() - buffer.position());
			System.out.println("W:" + w);
			buffer.put(msg, written, w);
			written += w;
			write();
			System.out.println("remaining: " + (length-written));
			if(written == length) {
				state = State.WRITING_IDLE;
				queue.poll();
				return true;
			}
				
		}
		
		return false;
	}

	public SelectionKey getKey(){
		return key;
	}
}
