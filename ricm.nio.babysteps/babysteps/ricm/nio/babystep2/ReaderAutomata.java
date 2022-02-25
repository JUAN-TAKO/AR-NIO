package ricm.nio.babystep2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ReaderAutomata {
	enum State {READING_LENGTH, READING_MSG} ;
	
	State state = State.READING_LENGTH;
	byte[] data;
	ByteBuffer buffer = ByteBuffer.allocate(256);
	int length;
	int bytes_read;
	
	public ReaderAutomata() {

	}
	
	private boolean read(SelectionKey key) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		buffer.rewind();
		int n = sc.read(buffer);
		if (n == -1) {
			key.cancel();  // communication with server is broken
			sc.close();
			return false;
		}
		buffer.rewind();
		return true;
	}
	
	public void handleRead(SelectionKey key) throws IOException{
				
		if (state == State.READING_LENGTH) {
			if(!read(key)) return;
			length = buffer.getInt();
			data = new byte[length];
			bytes_read = 0;
			System.out.println("length read " + length);
			state = State.READING_MSG;
		} 
		else if (state == State.READING_MSG) {
			if(!read(key)) return;
			int r = Math.min(data.length, length-bytes_read);
			System.out.println("get " + r);
			buffer.get(data, 0, r);
			bytes_read += r;
			System.out.println("bytes read " + bytes_read);
			System.out.println("remaining " + (length - bytes_read));
			if(bytes_read == length)
				state = State.READING_LENGTH;
		}
	}
	public boolean available() {
		return state == State.READING_LENGTH;
	}
	public byte[] get() {
		assert(available());
		return data;
	}
}
