package ext.gemalto.pdm.generic.xml.process;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import ext.gemalto.pdm.generic.xml.utils.WTBuilder;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;

public class ImportStructureProcess extends DefaultObjectFormProcessor {

	private static final Logger LOGGER = Logger.getLogger(ImportStructureProcess.class.getName());
	private FormResult formresult = new FormResult();
	private WTBuilder builder = null;
	private FeedbackMessage feedbackMessage=null;

	@Override
	public FormResult doOperation(NmCommandBean arg0, List<ObjectBean> arg1) {

		try {
			
			this.builder = new WTBuilder();
			LOGGER.log(Level.INFO, "Importing .... ");
			this.builder.importXmlStructure();
			
			
			if (WTBuilder.generatedObject != null) {
				// get the message for multi-object
				WTMessage messTitle = getSuccessMessageTitle();
				WTMessage mess = getSuccessMessageBody();
				ArrayList<Object> createdObjects = new ArrayList<Object>();
				createdObjects.add(WTBuilder.generatedObject);
				feedbackMessage = new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), messTitle,
						(ArrayList<String>) null, createdObjects, mess);
				formresult.addFeedbackMessage(feedbackMessage);
			} 
				
		} catch (WTException | ClassNotFoundException e) {
			LOGGER.log(Level.WARNING, "An error occured when importing object structure " + e.getMessage());
			formresult = new FormResult(FormProcessingStatus.FAILURE);
			feedbackMessage=new FeedbackMessage();
			feedbackMessage.addMessage(e.getMessage());
			formresult.addFeedbackMessage(feedbackMessage);
			
		}

		return formresult;
	}

}
