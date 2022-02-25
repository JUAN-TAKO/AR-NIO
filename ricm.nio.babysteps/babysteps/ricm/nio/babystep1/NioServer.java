package ricm.nio.babystep1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Iterator;

import ricm.nio.babystep2.IOAutomata;
import ricm.nio.babystep2.ReaderAutomata;
import ricm.nio.babystep2.WriterAutomata;

/**
 * NIO elementary server 
 * Implements an overly simplified echo server system
 * Polytech/Info4/AR 
 * Author: F. Boyer, O. Gruber
 */

public class NioServer {
	public static final int DEFAULT_SERVER_PORT = 8888;

	// The server channel to accept connections from clients
	private ServerSocketChannel ssc;
	
	// The selection key to register events of interests on the server channel
	private SelectionKey skey;

	// NIO selector
	private Selector selector;

	/**
	 * NIO server initialization
	 * 
	 * @param the host address and port of the server
	 * @throws IOException
	 */
	
	public NioServer(int port) throws IOException {

		// create a new selector
		selector = SelectorProvider.provider().openSelector();

		// create a new non-blocking server socket channel
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		// bind the server socket to the given address and port
		InetAddress hostAddress;
		hostAddress = InetAddress.getByName("localhost");
		InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
		ssc.socket().bind(isa);

		// register a ACCEPT interest for channel ssc
		skey = ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	/**
	 * NIO mainloop Wait for selected events on registered channels Selected events
	 * for a given channel may be ACCEPT, CONNECT, READ, WRITE Selected events for a
	 * given channel may change over time
	 */
	public void loop() throws IOException {
		System.out.println("NioServer running");
		while (true) {
			System.out.println("loop");
			// wait for some events
			selector.select();

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

	/**
	 * Handle an accept event - accept the connection and make it non-blocking
	 * 
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleAccept(SelectionKey key) throws IOException {
		assert (this.skey == key);
		assert (ssc == key.channel());
		SocketChannel sc;

		// do the actual accept on the server-socket channel
		// get a client channel as result
		sc = ssc.accept();
		sc.configureBlocking(false);
		// register a READ interest on sc to receive the message sent by the client
		SelectionKey k = sc.register(selector, SelectionKey.OP_READ);
		k.attach(new IOAutomata(new ReaderAutomata(k), new WriterAutomata(k)));
	}

	/**
	 * Handle a connect event, this should never happen
	 * 
	 * @param the key of the channel on which a connection is requested
	 * @throws Error since this should never happen
	 */
	private void handleConnect(SelectionKey key) throws IOException {
		throw new Error("Unexpected connect");
	}

	/**
	 * Handle incoming data 
	 * 
	 * @param the key of the channel on which the incoming data waits to be received
	 */
	private void handleRead(SelectionKey key) throws IOException {
		assert (skey != key);
		assert (ssc != key.channel());
		
		IOAutomata automata = (IOAutomata)key.attachment();
		
		if(automata.reader.handleRead()) {
			byte[] data = automata.reader.get();
			String msg = new String(data,Charset.forName("UTF-8"));
			System.out.println("NioServer received: " + msg);
			// echo back the same message to the client
			send(key, data);
		}
		
				
	}

	/**
	 * Handle outgoing data event
	 * 
	 * @param the key of the channel on which data can be sent
	 */
	private void handleWrite(SelectionKey key) throws IOException {
		assert (skey != key);
		assert (ssc != key.channel());

		IOAutomata automata = (IOAutomata)key.attachment();
		if(automata.writer.handleWrite())
			key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * Send data
	 * 
	 * @param the key of the channel on which data that should be sent
	 * @param the data that should be sent
	 * @throws IOException 
	 */
	public void send(SelectionKey key, byte[] data) throws IOException {
		
		SocketChannel sc = (SocketChannel) key.channel();
		IOAutomata automata = (IOAutomata) key.attachment();
		automata.writer.sendMsg(data);

		// register a write interest for the given client socket channel
		SelectionKey k = sc.keyFor(selector);
		k.interestOps(SelectionKey.OP_WRITE);
	}
	
	

	public static void main(String args[]) throws IOException {
		int serverPort = DEFAULT_SERVER_PORT;
		String arg;

		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.equals("-p")) {
				serverPort = new Integer(args[++i]).intValue();
			}
		}
		NioServer ns;
		ns = new NioServer(serverPort);
		ns.loop();
	}

}
