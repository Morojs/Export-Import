package ext.gemalto.pdm.generic.xml.process;

import java.io.IOException;
import java.util.List;
import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import ext.gemalto.pdm.generic.xml.utils.WTGenerator;
import wt.fc.ReferenceFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.util.WTException;


public class ExportStructureProcess extends DefaultObjectFormProcessor {
	private static final Logger LOGGER = Logger.getLogger(ExportStructureProcess.class.getName());
	private FormResult result = new FormResult();
	private WTPart rootPart = null;
	private WTGenerator builderXml = null;
	
	
	
	@Override
	public FormResult doOperation(NmCommandBean arg0, List<ObjectBean> arg1) throws WTException {

		try {
		
			String oid = arg0.getActionOid().getOidObject().toString();
			// WTPart Root Object
			rootPart = (WTPart) new ReferenceFactory().getReference(oid).getObject();
			builderXml = new WTGenerator(rootPart);
			builderXml.buildWTPartUsagesLinks();
			
			LOGGER.log(Level.INFO,"Exporting ...."+rootPart.getNumber());
			result.setStatus(FormProcessingStatus.SUCCESS);
			result.addFeedbackMessage(new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(), null, null,"Exported Successfully "));

		} catch (WTException | IOException e) {
			// TODO Auto-generated catch block
			LOGGER.log(Level.WARNING,"An error has occurred during export processing"+ e.getMessage());
			result.setStatus(FormProcessingStatus.FAILURE);
			result.addFeedbackMessage(new FeedbackMessage(FeedbackType.ERROR, SessionHelper.getLocale(), null, null,
					"An error has occurred during export processing | " + e.getMessage()));
		}

		return result;
	}


}
