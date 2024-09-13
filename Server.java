package prova.b;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Server extends Thread {
	private int connectionNumber;
	private boolean shutdown;
	private ArrayList<String> log;
	private ArrayList<Handler> clients;
	private HashMap<String, String> commands;
	private ServerSocket ws;
	private Semaphore mutex;
	private ExecutorService pool;
	private static int serverTimeoutSeconds = 3;

	public Server(int size) {
		this.connectionNumber = 1;
		this.shutdown = false;
		this.commands = new HashMap<String, String>();
		this.log = new ArrayList<>();
		initCommands();
		this.clients = new ArrayList<>();
		this.mutex =  new Semaphore(1);
		this.pool = Executors.newFixedThreadPool(size);

		try {
			this.ws = new ServerSocket(7979);
			ws.setSoTimeout(serverTimeoutSeconds * 1000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void initCommands() {
		// TODO Auto-generated method stub
		this.commands.put("cmdlist", "cmdlist returns the list of available commands.");
		this.commands.put("help", "help <command> returns the description of the command.");
		this.commands.put("con", "con returns the number of clients connected.");
		this.commands.put("exit", "exit is used to disconnect from the server.");
		this.commands.put("logaccept", "logaccept returns the list of all the connected clients.");
		this.commands.put("shutdown", "shutdown <sec> used to disconnect from the server");

	}

	public void printLog(PrintWriter out) {
		for(String line : this.log) {
			out.println(line);
		}
	}

	public boolean isShutdown() {
		return shutdown;
	}

	public int connectedClients() {
		return clients.size();
	}

	public void shutdown(int seconds) {

		pool.shutdown();
		shutdown = true;

		Timer timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask() {
			int i = seconds;

			public void run() {

				for(Handler client : clients) {
					client.shutdownMessage(i);
				}
				i--;

				if (i < 0) {
					timer.cancel();
				}
			}
		}, 0, 1000);
	
		
	}


	public void remove(Handler client) {
		try {
			mutex.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clients.remove(client);
		mutex.release();
	}


	public void run() {
		while(!shutdown){
			Socket s = null;
			Handler client = null;
			try {
				if(shutdown)
					break;
				System.out.println("Clients connected: " + clients.size());
				s = ws.accept();
				client = new Handler(s, mutex, commands, connectionNumber, this);
				try {
					mutex.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				clients.add(client);
				log.add(connectionNumber + " - (" + s.getInetAddress() + " - " + s.getPort() + ")");
				connectionNumber++;
				mutex.release();
				pool.execute(client);
			} catch (IOException e) {
				if(shutdown) {
					break;
				}

			}
		}
		try {
			ws.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
