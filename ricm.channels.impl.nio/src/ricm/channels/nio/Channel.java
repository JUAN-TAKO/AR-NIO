package ricm.channels.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import ricm.channels.IChannel;
import ricm.channels.IChannelListener;

public class Channel implements IChannel {

	IChannelListener listener;
	SelectionKey key;
	Writer writer;
	Reader reader;
	

	Channel(SelectionKey k){
		key = k;
		writer = new Writer(k);
		reader = new Reader(k);
	}

	@Override
	public void setListener(IChannelListener l) {
		listener = l;
		
	}

	@Override
	public void send(byte[] bytes, int offset, int count) {
		
	}

	@Override
	public void send(byte[] bytes) {
		try {
			writer.sendMsg(bytes);
			System.out.println("WRITE MODE");
			key.interestOps(SelectionKey.OP_WRITE);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void read(){
		try {
			if(reader.handleRead())
				listener.received(this, reader.get());
			else{
				System.out.println("READ MODE");
				key.interestOps(SelectionKey.OP_READ);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(){
		try {
			if(writer.handleWrite()) {
				System.out.println("READ MODE");
				key.interestOps(SelectionKey.OP_READ);
			}
		} catch (IOException e) {
			System.out.flush();
			System.exit(-1);
			e.printStackTrace();
		}
	}

	

	@Override
	public void close() {
		try {
			writer.getKey().channel().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean closed() {
		return writer.getKey().channel().isOpen();
	}
	
	// to complete
}
