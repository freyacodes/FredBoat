package fredboat.feature;

import fredboat.config.property.Credentials;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import net.dv8tion.jda.core.utils.tuple.ImmutablePair;
import net.dv8tion.jda.core.utils.tuple.Pair;

public class DikeSessionController extends SessionControllerAdapter {

    private final Credentials credentials;

    public DikeSessionController(Credentials credentials) {
        super();
        this.credentials = credentials;
    }

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
        return credentials.getDikeUrl();
    }

    @Override
    public Pair<String, Integer> getGatewayBot(JDA api) {
        Pair<String, Integer> pair = super.getGatewayBot(api);

        return new ImmutablePair<>(credentials.getDikeUrl(), pair.getRight());
    }

}
