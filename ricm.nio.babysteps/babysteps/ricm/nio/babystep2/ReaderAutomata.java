package ricm.nio.babystep2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ReaderAutomata {
	enum State {READING_LENGTH, READING_MSG} ;
	
	State state = State.READING_LENGTH;
	byte[] data;
	ByteBuffer buffer = ByteBuffer.allocate(64);
	int length;
	int bytes_read;
	SelectionKey key;
	
	public ReaderAutomata(SelectionKey k) {
		key = k;
	}
	
	private boolean read() throws IOException {
		System.out.println("f read");
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
	
	public boolean handleRead() throws IOException{
		System.out.println("handle read");
		if (state == State.READING_LENGTH) {
			if(!read()) return false;
			length = buffer.getInt();
			data = new byte[length];
			bytes_read = 0;
			System.out.println("length read " + length);
			state = State.READING_MSG;
		} 
		else if (state == State.READING_MSG) {
			int u = buffer.capacity() - buffer.position();
			if(u <= 0)
				read();
			u = buffer.capacity() - buffer.position();
			int r = Math.min(u, length - bytes_read);
			buffer.get(data, bytes_read, r);
			bytes_read += r;
			System.out.println("bytes read " + bytes_read);
			System.out.println("remaining " + (length - bytes_read));
			
			if(bytes_read == length) {
				state = State.READING_LENGTH;
				return true;
			}
		}
		return false;
	}
	public boolean available() {
		return state == State.READING_LENGTH;
	}
	public byte[] get() {
		assert(available());
		System.out.println("get");
		return data;
	}
}
