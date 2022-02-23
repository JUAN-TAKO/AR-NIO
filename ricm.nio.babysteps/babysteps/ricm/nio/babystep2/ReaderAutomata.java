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
	int to_read;
	
	private boolean read(SelectionKey key) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		int n = sc.read(buffer);
		if (n == -1) {
			key.cancel();  // communication with server is broken
			sc.close();
			return false;
		}
		buffer.rewind();
		return true;
	}
	
	void handleRead(SelectionKey key) throws IOException{
				
		if (state == State.READING_LENGTH) {
			if(!read(key)) return;
			length = buffer.getInt();
			data = new byte[length];
			to_read = length;
			
			state = State.READING_MSG;
		} 
		else if (state == State.READING_MSG) {
			if(!read(key)) return;
			int r = Math.min(data.length, to_read);
			buffer.get(data, 0, r);
			to_read -= r;
			if(to_read == 0)
				state = State.READING_LENGTH;
		}
	}
}
