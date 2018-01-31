package fredboat.feature;

import fredboat.main.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import net.dv8tion.jda.core.utils.tuple.ImmutablePair;
import net.dv8tion.jda.core.utils.tuple.Pair;

public class DikeSessionController extends SessionControllerAdapter {

    @Override
    public void appendSession(SessionConnectNode node) {
        try {
            node.run(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeSession(SessionConnectNode node) {
        // Nop
    }

    @Override
    public String getGateway(JDA api) {
        return Config.CONFIG.getDikeUrl();
    }

    @Override
    public Pair<String, Integer> getGatewayBot(JDA api) {
        Pair<String, Integer> pair = super.getGatewayBot(api);

        return new ImmutablePair<>(Config.CONFIG.getDikeUrl(), pair.getRight());
    }

}
