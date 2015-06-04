package com.instaclick.pentaho.plugin.amqp;

import java.io.IOException;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import static com.instaclick.pentaho.plugin.amqp.Messages.getString;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.trans.TransListener;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.RowListener;
import org.pentaho.di.core.exception.KettleValueException;

public class ConfirmationRowStepWatcher implements RowListener {
    int rowsRead = 0;
    private String deliveryTagName = null;
    private AMQPConfirmation   ackDelegate;
    private AMQPConfirmation   rejectDelegate;

    public ConfirmationRowStepWatcher(String deliveryTagName) {
        this.deliveryTagName = deliveryTagName;
    }

    @Override
    public void rowReadEvent( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException
    {
        Long deliveryTag=-1L;
        try {
            deliveryTag = rowMeta.getInteger(row,rowMeta.indexOfValue(deliveryTagName));
            rowsRead++;

            if ( ackDelegate != null ) {
                synchronized (this) {
                    ackDelegate.ackDelivery(deliveryTag);
                }
            }

            if ( rejectDelegate != null ) {    
                synchronized (this) {
                    rejectDelegate.rejectDelivery(deliveryTag);
                }
            }


        } catch (KettleValueException e) { 
            throw new KettleStepException(e);
        } catch (IOException e) {
            throw new KettleStepException(e);
        };

        
    }

    public void rowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) {
    }

    public void errorRowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) {
    }

    public void setAckDelegate(BaseStep step, AMQPConfirmation deleg) throws AMQPException
    { 
        step.logDebug("SET ACK DELEGATE");

        if (ackDelegate != null )  {
            throw new AMQPException("Already set Acknowledger Delegate.");
        }
        if (rejectDelegate != null )  {
            throw new AMQPException("Already set Reject Delegate. SIMULTANOUES ack is impossible");
        }
        ackDelegate = deleg;
    }

    public void setRejectDelegate(BaseStep step, AMQPConfirmation deleg) throws AMQPException
    { 
        step.logDebug("SET REJECT DELEGATE");

        if (rejectDelegate != null ) {
            throw new AMQPException("Already set Reject Delegate.");
        }
        if (ackDelegate != null )  {
            throw new AMQPException("Already set Acknowledger Delegate. SIMULTANOUES ack is impossible");
        }
        rejectDelegate = deleg;
    }



}

