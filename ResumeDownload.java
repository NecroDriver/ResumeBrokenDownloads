import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class ResumeDownload {
	public static final String DOWNLOAD_URL = "http://7xs0af.com1.z0.glb.clouddn.com/High-Wake.mp3";
	public static final String DOWNLOAD_PARENT_PATH = "D:\\test_resume_download\\hi";
	public static final int THREAD_COUNT = 3;

	public static void main(String[] args) {
		try {
			// ��ȡ�����ص�ַ������
			URL mUrl = new URL(DOWNLOAD_URL);
			HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
			// ��ȡ�����ļ��Ĵ�С
			int fileLen = conn.getContentLength();
			// ͨ���������ӻ�ȡ�����ļ����ļ���
			String filePathUrl = conn.getURL().getFile();
			String fileName = filePathUrl.substring(filePathUrl.lastIndexOf(File.separator) + 1);
			// ��������·��
			String fileDownloadPath = DOWNLOAD_PARENT_PATH + File.separator + fileName;
			// �жϸ�·���Ƿ���ڣ������ھ�����
			File file = new File(fileDownloadPath);
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			// �ر�����
			conn.disconnect();

			/**
			 * ����Ϊ���߳����أ���Ҫԭ����ǽ��ļ���С���ֶ�飨�����߳����� ÿһ���̴߳Ӳ�ͬ����ʼλ�ã�������ȴ�С���ļ� ��Ҫͨ��
			 * HttpUrlConnection��������Range����������ÿһ���߳����صķ�Χ
			 * setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
			 */

			int blockSize = fileLen / THREAD_COUNT;
			for (int threadId = 1; threadId <= THREAD_COUNT; threadId++) {
				// ��ȡÿһ���߳����ص���ʼλ�úͽ���λ��
				long startPos = (threadId - 1) * blockSize;
				long endPos = threadId * blockSize - 1;
				if (threadId == THREAD_COUNT) {
					endPos = fileLen;
				}

				// Ȼ��ͨ���ٲ�ͬ�߳�����ʵ�������߼�
				// ����ʵ����DownloadThread���Runnable����
				new Thread(new DownLoadTask(threadId, startPos, endPos, fileDownloadPath, DOWNLOAD_URL)).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

/**
 * ���������߼�
 * 
 * @author Administrator
 *
 */
class DownLoadTask implements Runnable {
	public static final String TEMP_NAME = "_tempfile";
	private int threadId; // ��ǰ�߳�id
	private long startPos; // ���ص���ʼλ��
	private long endPos; // ���صĽ���λ��
	private String fileDownloadPath; // �����ļ���ŵ��ļ�λ��
	private String downloadUrl; // ��������

	private String tempFilePath; // ��¼���ȵ���ʱ�ļ�·��

	public DownLoadTask(int threadId, long startPos, long endPos, String fileDownloadPath, String downloadUrl) {
		super();
		this.threadId = threadId;
		this.startPos = startPos;
		this.endPos = endPos;
		this.fileDownloadPath = fileDownloadPath;
		this.downloadUrl = downloadUrl;

		this.tempFilePath = fileDownloadPath + TEMP_NAME + threadId;
	}

	@Override
	public void run() {
		try {
			// ��¼���صĿ�ʼʱ��
			long startTime = System.currentTimeMillis();

			URL mUrl = new URL(downloadUrl);

			// Ϊ��ʵ�ֶϵ����أ�����������ʱ�ӻ����ļ������ȡ���ص���ʼλ��
			if (getProgress(threadId) != 0) {
				startPos = getProgress(threadId);
			}

			System.out.println("�߳�" + threadId + "�������أ���ʼλ�ã�" + startPos + "����λ���ǣ�" + endPos);

			// HttpUrlConnection�ĳ������
			// Ҫʵ�ֶϵ����صĻ�������Ҫ����mConnection.setRequestProperty("Range", "bytes=" +
			// startPos + "-" + endPos);
			HttpURLConnection mConnection = (HttpURLConnection) mUrl.openConnection();
			mConnection.setRequestMethod("POST");
			mConnection.setReadTimeout(5000);
			mConnection.setRequestProperty("Charset", "UTF-8");
			mConnection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
			mConnection.connect();

			// �������·�������ڵĻ����򴴽��ļ�·��
			File file = new File(fileDownloadPath);
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}

			// ͨ��RandomAccessFile��Ҫ���ص��ļ����ж�д
			RandomAccessFile downloadFile = new RandomAccessFile(fileDownloadPath, "rw");
			// д��ʱ�򣬽�����Ƶ�Ҫ���ص���ʼλ��
			downloadFile.seek(startPos);

			BufferedInputStream bis = new BufferedInputStream(mConnection.getInputStream());
			int size = 0; // ��ȡ��������ŵ��ֽڴ�С
			long len = 0; // ��¼�������صĴ�С���Ա���㱾�����ص���ʼλ���ƶ���������
			byte[] buf = new byte[1024];
			while ((size = bis.read(buf)) != -1) {
				// �ۼ�
				len += size;
				// Ȼ�󽫻�����������д�������ļ���
				downloadFile.write(buf, 0, size);
				// Ȼ�����ص���ʼλ���ƶ����Ѿ��������ĩβ��д�������ļ�����ȥ
				setProgress(threadId, startPos + len);
			}

			// ��ȡ���ؽ���ʱ�䣬���
			long curTime = System.currentTimeMillis();
			System.out.println("�߳�" + threadId + "�Ѿ�������ɣ���ʱ��" + (curTime - startTime) + "ms.");

			// �ر������ļ�������
			downloadFile.close();
			mConnection.disconnect();
			bis.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��temp�ļ���ȡ���ؽ���
	 * 
	 * @param threadId
	 * @return
	 */
	private long getProgress(int threadId) {
		try {
			File markFile = new File(tempFilePath);
			if (!markFile.exists()) {
				return 0;
			}
			FileInputStream fis = new FileInputStream(markFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			byte[] buf = new byte[1024];
			String startPos = "";
			int len = -1;
			while ((len = bis.read(buf)) != -1) {
				startPos += new String(buf, 0, len);
			}

			// ���ر����Ļ�������ɾ���ļ�
			fis.close();
			bis.close();

			return Long.parseLong(startPos);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * ��temp�ļ���¼���ؽ���
	 * 
	 * @param threadId
	 * @param startPos
	 */
	private void setProgress(int threadId, long startPos) {
		try {
			File markFile = new File(tempFilePath);
			if (!markFile.getParentFile().exists()) {
				markFile.getParentFile().mkdirs();
			}
			
			RandomAccessFile rr = new RandomAccessFile(markFile, "rw");// �洢���ر�ǵ��ļ�
			String strStartPos = String.valueOf(startPos);
			rr.write(strStartPos.getBytes(), 0, strStartPos.length());
			
			rr.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// ���ļ��������ʱ������ʼλ�úͽ���λ���غ�ʱ��ɾ����¼���ȵĻ����ļ�
			if (startPos >= endPos) {
				File markFile = new File(tempFilePath);
				if (markFile.exists()) {
					System.out.println("markFile delete");
					markFile.delete();
				}
			}
		}

	}
}
