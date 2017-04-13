
package info.rmapproject.loader.deposit.disco;

import java.util.function.Consumer;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.rmapproject.loader.HarvestRecord;
import info.rmapproject.loader.jms.HarvestRecordListener;
import info.rmapproject.loader.jms.JmsClient;

/**
 * Pulls from a queue, invokes a deposit action, and sends to an error queue if a failure occurs.
 * <p>
 * This manages the messaging aspect of depositing DiSCOs. It consumes messages containing {@link HarvestRecord} from
 * a queue (typically named <code>rmap.harvest.disco.*</code>), and passes it on to a provided {@link Consumer} of
 * harvest records to perform a deposit. If an exception is thrown by the consumer, a stack trace of the exception is
 * added to the message, and the message is routed to an error queue <code>rmap.harvest.error.disco*</code>
 * </p>
 *
 * @author apb@jhu.edu
 */
public class DiscoDepositService implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoDepositService.class);

    private JmsClient jms;

    private ConnectionFactory connectionFactory;

    private String queueSpec;

    private Consumer<HarvestRecord> discoDeposit;

    public void setConnectionFactory(ConnectionFactory factory) {
        this.connectionFactory = factory;
    }

    public void setQueueSpec(String spec) {
        this.queueSpec = spec;
    }

    public void setDiscoConsumer(Consumer<HarvestRecord> consumer) {
        this.discoDeposit = consumer;
    }

    public void start() {

        jms = new JmsClient(connectionFactory);

        jms.listen(queueSpec,
                new HarvestRecordListener(discoDeposit)
                        .withExceptionHandler((m, e) -> {
                            try {
                                final String dest = errorDestination(m.getJMSDestination().toString());

                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Record listener threw an exception", e);
                                }
                                LOG.info("Record listener threw an exception, routing to {}", dest);

                                jms.write(dest, m);
                            } catch (final JMSException j) {
                                throw new RuntimeException("Error placing into error queue: " + j.getMessage());
                            }
                        }));

        LOG.info("Disco deposit service started for " + queueSpec);
    }

    private String errorDestination(String src) {
        return src.replace("queue://", "").replaceFirst("disco", "error.disco");
    }

    @Override
    public void close() throws Exception {
        jms.close();
    }
}
