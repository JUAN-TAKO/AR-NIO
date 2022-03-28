package ricm.channels.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import ricm.channels.IBroker;
import ricm.channels.IBrokerListener;

/**
 * Broker implementation
 */

public class Broker implements IBroker {

	private Selector selector;
	private ServerSocketChannel ssc;
	private IBrokerListener listener;
	private int port;

	enum State{IDLE, SERVER, CLIENT};
	private State state = State.IDLE;

	public Broker() throws Exception {
		// to complete
	}

	@Override
	public void setListener(IBrokerListener l) {
		listener = l;
	}

	@Override
	public boolean connect(String host, int port) {
		try {
			if(state == State.IDLE){
				state = State.CLIENT;	
			}
			else{
				System.out.println("Unexpected connect");
				listener.refused(host, port);
				return false;
			}
			
			selector = SelectorProvider.provider().openSelector();

			// create a non-blocking server socket channel
			SocketChannel sc = SocketChannel.open();
			sc.configureBlocking(false);

			System.out.println("CONNECT MODE");
			// register a CONNECT interest for channel sc 
			SelectionKey skey = sc.register(selector, SelectionKey.OP_CONNECT);
			
			// request to connect to the server
			InetAddress addr;
			addr = InetAddress.getByName(host);
			
			sc.connect(new InetSocketAddress(addr, port));
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			listener.refused(host, port);
			return false;
		}
	}

	@Override
	public boolean accept(int port) {

		if(state != State.IDLE){
			System.out.println("Unexpected accept");
			return false;
		}

		try {
			state = State.SERVER;

			selector = SelectorProvider.provider().openSelector();
			// create a new non-blocking server socket channel
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);

			// bind the server socket to the given address and port
			InetAddress hostAddress;
			hostAddress = InetAddress.getByName("localhost");
			InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
			ssc.socket().bind(isa);

			System.out.println("ACCEPT MODE");
			// register a ACCEPT interest for channel ssc
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}

	private void handleAccept(SelectionKey key){
		System.out.println("HANDLE ACCEPT");
		try {
			// do the actual accept on the server-socket channel
			// get a client channel as result
			SocketChannel sc = ssc.accept();
			sc.configureBlocking(false);
			
			System.out.println("READ MODE");
			// register a READ interest on sc to receive the message sent by the client
			SelectionKey k = sc.register(selector, SelectionKey.OP_READ);

			Channel ch = new Channel(k);
			k.attach(ch);
			listener.accepted(ch);

		} catch (ClosedChannelException e) {
			e.printStackTrace(); return;
		} catch (IOException e){
			e.printStackTrace(); return;
		}
		
	}

	private void handleRead(SelectionKey k){
		System.out.println("HANDLE READ");
		Channel ch = (Channel)k.attachment();
		ch.read();
	}

	private void handleWrite(SelectionKey k){
		System.out.println("HANDLE WRITE");
		Channel ch = (Channel)k.attachment();
		ch.write();
	}

	private void handleConnect(SelectionKey k){
		System.out.println("HANDLE CONNECT");
		SocketChannel sc = (SocketChannel)k.channel();

		try {
			sc.finishConnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("READ MODE");
		k.interestOps(SelectionKey.OP_READ);
		Channel ch = new Channel(k);
		k.attach(ch);
		listener.connected(ch);
	}

	public void run() {
		System.out.println("Broker running");
		while (true) {
			System.out.println("loop");
			// wait for some events
			try {
				selector.select();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			// get the keys for which the events occurred
			Iterator<?> selectedKeys = this.selector.selectedKeys().iterator();

			while (selectedKeys.hasNext()) {

				SelectionKey key = (SelectionKey) selectedKeys.next();
				selectedKeys.remove();
				
				// process the event
				if (key.isValid() && key.isAcceptable())  // accept event
					handleAccept(key);
				if (key.isValid() && key.isReadable())    // read event
					handleRead(key);
				if (key.isValid() && key.isWritable())    // write event
					handleWrite(key);
				if (key.isValid() && key.isConnectable())  // connect event
					handleConnect(key);
			}
		}
		
	}

}
