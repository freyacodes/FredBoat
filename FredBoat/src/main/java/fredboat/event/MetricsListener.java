/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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
 */

package fredboat.event;

import io.prometheus.client.Counter;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.http.HttpRequestEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created by napster on 11.09.17.
 * <p>
 * General event receving metrics. See {@link EventListenerBoat} for message event metrics handling
 */
public class MetricsListener extends ListenerAdapter {

    public static MetricsListener getSingleton() {
        return MetricsListenerHolder.INSTANCE;
    }

    //holder pattern singleton
    private static class MetricsListenerHolder {
        private static final MetricsListener INSTANCE = new MetricsListener();
    }


    private static final Counter guildJoins = new Counter.Builder()
            .name("fredboat_events_guild_joins_total")
            .help("Total guild joins")
            .register();

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guildJoins.inc();
    }


    private static final Counter guildLeaves = new Counter.Builder()
            .name("fredboat_events_guild_leaves_total")
            .help("Total guild leaves")
            .register();

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        guildLeaves.inc();
    }

    private static final Counter jdaReadys = new Counter.Builder()
            .name("fredboat_events_jda_ready_total")
            .help("Total JDA ready events")
            .register();

    @Override
    public void onReady(ReadyEvent event) {
        jdaReadys.inc();
    }


    private static final Counter jdaResumes = new Counter.Builder()
            .name("fredboat_events_jda_resume_total")
            .help("Total JDA resume events")
            .register();

    @Override
    public void onResume(ResumedEvent event) {
        jdaResumes.inc();
    }

    private static final Counter jdaReconnects = new Counter.Builder()
            .name("fredboat_events_jda_reconnect_total")
            .help("Total JDA reconnect events")
            .register();

    @Override
    public void onReconnect(ReconnectedEvent event) {
        jdaReconnects.inc();
    }

    private static final Counter jdaDisconnects = new Counter.Builder()
            .name("fredboat_events_jda_disconnect_total")
            .help("Total JDA disconnect events")
            .register();

    @Override
    public void onDisconnect(DisconnectEvent event) {
        jdaDisconnects.inc();
    }

    private static final Counter jdaShutdowns = new Counter.Builder()
            .name("fredboat_events_jda_shutdown_total")
            .help("Total JDA shutdown events")
            .register();

    @Override
    public void onShutdown(ShutdownEvent event) {
        jdaShutdowns.inc();
    }

    private static final Counter jdaExceptions = new Counter.Builder()
            .name("fredboat_events_jda_exception_total")
            .help("Total JDA exception events")
            .register();

    @Override
    public void onException(ExceptionEvent event) {
        jdaExceptions.inc();
    }


    private static final Counter jdaEvents = new Counter.Builder()
            .name("fredboat_events_jda_total")
            .help("Total JDA events")
            .register();

    @Override
    public void onGenericEvent(Event event) {
        jdaEvents.inc();
    }

    private static final Counter jdaHttpRequests = new Counter.Builder()
            .name("fredboat_events_jda_http_requests_total")
            .help("Total JDA http requests events")
            .register();

    @Override
    public void onHttpRequest(HttpRequestEvent event) {
        jdaHttpRequests.inc();
    }
}
