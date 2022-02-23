package ricm.nio.babystep2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ReaderAutomata {
	enum State {READING_LENGTH, READING_MSG} ;
	
	State state = State.READING_LENGTH;
	byte[] data;
	ByteBuffer buffer = ByteBuffer.allocate(4);
	int length;
	
	private int read(SelectionKey key) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		int n = sc.read(buffer);
		if (n == -1) {
			key.cancel();  // communication with server is broken
			sc.close(); 		
		}
		return n;
	}
	void handleRead(SelectionKey key) throws IOException{
				
		if (state == State.READING_LENGTH) {
			
			buffer.rewind();
			read(key);
			length = buffer.getInt();
			data = new byte[length];
			
			state = State.READING_MSG;
		} 
		else if (state == State.READING_MSG) {
			
			buffer.rewind();
			buffer = ByteBuffer.allocate(Math.max(length, 4));
			buffer.get(data);
			
			state = State.READING_LENGTH;
		}
	}
}
