package servlet;

import entity.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ChatServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected HashMap<String, ChatUser> activeUsers;
    protected ArrayList<ChatMessage> messages;

    private Thread jokeThread;
    private int jokePeriod = 60 * 1000;

    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        super.init();
        activeUsers = (HashMap<String, ChatUser>)
                getServletContext().getAttribute("activeUsers");
        messages = (ArrayList<ChatMessage>)
                getServletContext().getAttribute("messages");

        if (activeUsers == null) {
            activeUsers = new HashMap<String, ChatUser>();
            getServletContext().setAttribute("activeUsers", activeUsers);
        }
        if (messages == null) {
            messages = new ArrayList<ChatMessage>(100);
            getServletContext().setAttribute("messages", messages);
        }

        String jkTimeout = getServletContext().getInitParameter("jokeTimeout");
        if (jkTimeout!=null) {
            jokePeriod = Integer.parseInt(jkTimeout) * 60 * 1000;
        }

        jokeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    for (ChatUser user : activeUsers.values()) {
                        System.out.println(Calendar.getInstance().getTimeInMillis() - user.getLastInteractionTime());
                        if (Calendar.getInstance().getTimeInMillis() - user.getLastInteractionTime() > jokePeriod) {
                            synchronized (messages) {
                                messages.add(new ChatMessage(JokesService.getRandomJoke(), user,
                                        Calendar.getInstance().getTimeInMillis()));
                                user.setLastInteractionTime(Calendar.getInstance().getTimeInMillis());
                            }
                        }
                    }
                    try {
                        Thread.sleep(1000);
                        System.out.println("sleep");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        jokeThread.start();

    }

    @Override
    public void destroy() {
        super.destroy();
        jokeThread.interrupt();
    }
}
