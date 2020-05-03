package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entity.*;

public class LoginServlet extends ChatServlet {
    private static final long serialVersionUID = 1L;
    private int sessionTimeout = 10*60;
    private Thread jokeThread;
    private int jokePeriod = 60 * 1000;

    @Override
    public void init() throws ServletException {
        super.init();
        String value = getServletConfig().getInitParameter("SESSION_TIMEOUT");
        if (value!=null) {
            sessionTimeout = Integer.parseInt(value);
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
                        System.out.println(user.getLastInteractionTime() - Calendar.getInstance().getTimeInMillis());
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

    protected void doGet(HttpServletRequest request, HttpServletResponse
            response) throws ServletException, IOException {
        String name = (String)request.getSession().getAttribute("name");
        String errorMessage =
                (String)request.getSession().getAttribute("error");
        String previousSessionId = null;
        if (name==null) {

            for (Cookie aCookie: request.getCookies()) {
                if (aCookie.getName().equals("sessionId")) {
                    previousSessionId = aCookie.getValue();
                    break;
                }
            }
            if (previousSessionId!=null) {
                for (ChatUser aUser: activeUsers.values()) {
                    if
                    (aUser.getSessionId().equals(previousSessionId)) {
                        name = aUser.getName();
                        aUser.setSessionId(request.getSession().getId());
                    }
                }
            }
        }
        if (name!=null && !"".equals(name)) {
            errorMessage = processLogonAttempt(name, request, response);
        }
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<html><head><title>Мега-чат!</title><link rel=\"stylesheet\" type=\"text/css\" href=\"/resource/styles.css\"><meta http-equiv='Content-Type' content='text/html; charset=utf-8'/></head>");
        if (errorMessage!=null) {
            pw.println("<p><font color='red'>" + errorMessage +
                    "</font></p>");
        }
        pw.println("<form action='/chat/' method='post'>Введите имя: <input type='text' name='name' value=''><input type='submit' value='Войти в чат'>");
        pw.println("</form></body></html>");
        request.getSession().setAttribute("error", null);
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse
            response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String name = (String)request.getParameter("name");
        String errorMessage = null;
        if (name==null || "".equals(name)) {
            errorMessage = "Имя пользователя не может быть пустым!";
        } else {
            errorMessage = processLogonAttempt(name, request, response);
        }
        if (errorMessage!=null) {
            request.getSession().setAttribute("name", null);
            request.getSession().setAttribute("error", errorMessage);
            response.sendRedirect(response.encodeRedirectURL("/chat/"));
        }
    }
    String processLogonAttempt(String name, HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        String sessionId = request.getSession().getId();
        ChatUser aUser = activeUsers.get(name);
        if (aUser==null) {
            aUser = new ChatUser(name,
                    Calendar.getInstance().getTimeInMillis(), sessionId);
            synchronized (activeUsers) {
                activeUsers.put(aUser.getName(), aUser);
            }
        }
        if (aUser.getSessionId().equals(sessionId) ||
                aUser.getLastInteractionTime()<(Calendar.getInstance().getTimeInMillis()-
                        sessionTimeout*1000)) {
            request.getSession().setAttribute("name", name);
            aUser.setLastInteractionTime(Calendar.getInstance().getTimeInMillis());
            Cookie sessionIdCookie = new Cookie("sessionId", sessionId);
            sessionIdCookie.setMaxAge(60*60*24*365);
            response.addCookie(sessionIdCookie);
            response.sendRedirect(response.encodeRedirectURL("/resource/view.htm"));
            return null;
        } else {
            return "Извините, но имя <strong>" + name + "</strong> уже кем-то занято. Пожалуйста выберите другое имя!";
        }
    }
}