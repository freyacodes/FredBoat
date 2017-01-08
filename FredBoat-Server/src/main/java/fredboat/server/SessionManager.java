/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.server;

import fredboat.common.Crypto;
import fredboat.common.db.DatabaseManager;
import fredboat.common.db.entities.UserSession;
import java.util.List;
import javax.persistence.EntityManager;

public class SessionManager {
    
    public static void mergeSession(UserSession us){
        EntityManager em = DatabaseManager.getEntityManager();
        
        em.getTransaction().begin();
        em.merge(us);
        em.getTransaction().commit();
    }
    
    public static UserSession findSession(long id) {
        EntityManager em = DatabaseManager.getEntityManager();
        
        return em.find(UserSession.class, id);
    }
    
    public static UserSession findSession(String token){
        EntityManager em = DatabaseManager.getEntityManager();
        List list = em.createQuery("SELECT ses FROM user_session ses WHERE ses.token = :token").setParameter("token", Crypto.hash(token)).getResultList();

        if (list.isEmpty()) {
            return null;
        }

        return (UserSession) list.get(0);
    }
    
}
