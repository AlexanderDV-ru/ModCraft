package ru.alexanderdv.modcraft;

import java.beans.ExceptionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import ru.alexanderdv.utils.ExceptionsHandler.VoidExceptionRunnable;
import ru.alexanderdv.utils.MessageSystem.Msgs;

public class Networking {
	String name;
	boolean stopped;
	ServerSocket server;
	Socket socket;
	BufferedWriter writer;
	BufferedReader reader;

	public void startServer(String name, int port) throws Exception {
		this.name = name;
		server = new ServerSocket(port);
		new Timer(1000, (a) -> {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				Msgs.last.debug(e);
			}
		}).start();
		for (Socket s; !stopped && !server.isClosed();)
			try {
				//Msgs.last.debug("try to accept");
				initSocket(s = server.accept());
				if (s == null)
					continue;
				//Msgs.last.debug("accepted");
				socketReader(socket);
			} catch (Exception e) {
				Msgs.last.debug(e);
			}
	}

	public ConcurrentHashMap<String, String> writeRequests = new ConcurrentHashMap<String, String>();

	public void startClient(String name, String ip, int port) throws Exception {
		this.name = name;
		initSocket(new Socket(ip, port));
		for (String request : writeRequests.keySet())
			try {
				write(request);
			} catch (Exception e) {
				// TODO: handle exception
			}
		writeRequests.clear();
		// writer.flush();
		closeAll();
	}

	public void socketReader(Socket socket) throws Exception {
		for (; !socket.isClosed() && socket.isConnected();)
			try {
				read();
			} catch (Exception e) {}
	}

	public void initSocket(Socket socket) throws Exception {
		this.socket = socket;
		if (reader != null)
			reader.close();
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		if (writer != null)
			writer.close();
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}

	public ConcurrentHashMap<String, String> readenRequests = new ConcurrentHashMap<String, String>();

	public void write(String line) throws IOException {
		writer.write(name + ":" + line + "\n");
		//Msgs.last.debug("[Write:] " + line);
	}

	public String read() throws IOException {
		String line = reader.readLine();
		if (line != null) {
			String name = line.split(":")[0];
			line = line.split(":")[1];
			//Msgs.last.debug("[Read from '" + name + "'] " + line);
			readenRequests.put(line, name);
			socket.close();
		}
		return line;
	}

	public void closeAll() throws IOException {
		if (writer != null)
			writer.close();
		if (reader != null)
			reader.close();
		if (socket != null)
			socket.close();
		if (server != null)
			server.close();
		if (networkThread != null)
			networkThread.interrupt();
	}

	Thread networkThread;

	public void startInExceptionThread(VoidExceptionRunnable runnable, ExceptionListener... handlers) {
		networkThread = new Thread(() -> {
			try {
				runnable.runWithThrows();
			} catch (Exception e) {
				if (handlers.length == 0)
					e.printStackTrace();
				for (ExceptionListener handler : handlers)
					handler.exceptionThrown(e);
			}
		});
		networkThread.start();
	}
}
