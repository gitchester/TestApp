package com.sample.home.testapp.slowmotion;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sample.home.testapp.BaseFragment;
import com.sample.home.testapp.R;
import com.sample.home.testapp.slowmotion.test.IFrameCallback;
import com.sample.home.testapp.slowmotion.test.MediaMoviePlayer;
import com.sample.home.testapp.slowmotion.test.PlayerTextureView;
import com.sample.home.testapp.slowmotion.test.androidtranscoder.MediaTranscoder;
import com.sample.home.testapp.slowmotion.test.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Created by chuongtran on 9/19/17.
 */

public class VideoSlowMotion extends BaseFragment {

    PlayerTextureView mPlayerView;
    ImageButton mPlayerButton;
    TextView txtProgress;
    MediaMoviePlayer mPlayer;

    private static final String TAG = "TranscoderActivity";
    private static final String FILE_PROVIDER_AUTHORITY = "net.ypresto.androidtranscoder.example.fileprovider";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;
    private Future<Void> mFuture;


    public VideoSlowMotion() {
        this.title = "Video Slow Motion";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.video_slow_motion, container, false);
        view.findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTesting();
            }
        });
        mPlayerView = (PlayerTextureView) view.findViewById(R.id.player_view);
        mPlayerView.setAspectRatio(640 / 480.f);
        mPlayerButton = (ImageButton) view.findViewById(R.id.play_button);
        mPlayerButton.setOnClickListener(mOnClickListener);

        txtProgress = view.findViewById(R.id.txtProgress);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPlayerView.onResume();
    }

    @Override
    public void onPause() {
        stopPlay();
        mPlayerView.onPause();
        super.onPause();
    }

    public void onStart() {
        super.onStart();
    }

    public void onStop() {
        super.onStop();
    }

    private void startTesting() {
        final long startTime = SystemClock.uptimeMillis();
        MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
            @Override
            public void onTranscodeProgress(double progress) {
                txtProgress.setText("Transcoding... " + String.valueOf(Math.round(progress * 100)) + "%");
                Logger.getAnonymousLogger().info("chuong-xxxxxx progress=" + progress);
            }

            @Override
            public void onTranscodeCompleted() {
                Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                onTranscodeFinished(true, "transcoded file placed on file");
                txtProgress.setText("Finished");
            }

            @Override
            public void onTranscodeCanceled() {
                onTranscodeFinished(false, "Transcoder canceled.");
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                onTranscodeFinished(false, "Transcoder error occurred.");
            }
        };

        File cacheDir;
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            cacheDir = new File(
                    android.os.Environment.getExternalStorageDirectory(),
                    "Android/media/com.sample.home.testapp");
        } else {
            // it does not have an SD card
            cacheDir = getContext().getCacheDir();
        }

        if (!cacheDir.exists())
            cacheDir.mkdirs();

        final File path = new File(cacheDir, "test.mp4");
        try {
            prepareSampleMovie(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final File output = new File(cacheDir, "result.mp4");
        try {
            mFuture = MediaTranscoder.getInstance().transcodeVideo(path.getAbsolutePath(), output.getAbsolutePath(),
                    MediaFormatStrategyPresets.createAndroid720pStrategy(8000 * 1000, 128 * 1000, 1), listener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * method when touch record button
     */
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.play_button:
                    if (mPlayer == null)
                        startPlay();
                    else
                        stopPlay();
//                    DecodeEditEncodeTest test = new DecodeEditEncodeTest();
//                    test.setContext(getContext());
//                    try {
//                        test.testVideoEdit720p();
//                    } catch (Throwable throwable) {
//                        throwable.printStackTrace();
//                    }
                    break;
            }
        }
    };

    /**
     * start playing
     */
    private void startPlay() {
        final Activity activity = getActivity();
        try {
            final File dir = activity.getFilesDir();
            if (!dir.exists())
                dir.mkdirs();
            final File path = new File(dir, "test.mp4");
            prepareSampleMovie(path);
            mPlayerButton.setColorFilter(0x7fff0000);
            mPlayer = new MediaMoviePlayer(getContext(), mPlayerView.getSurface(), mIFrameCallback, true);
            mPlayer.prepare(path.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * request stop playing
     */
    private void stopPlay() {
        mPlayerButton.setColorFilter(0);    // return to default color
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            // you should not wait here
        }
    }

    /**
     * callback methods from decoder
     */
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onPrepared() {
            final float aspect = mPlayer.getWidth() / (float) mPlayer.getHeight();
            final Activity activity = getActivity();
            if ((activity != null) && !activity.isFinishing())
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayerView.setAspectRatio(aspect);
                    }
                });
            mPlayer.play();
        }

        @Override
        public void onFinished() {
            mPlayer = null;
            final Activity activity = getActivity();
            if ((activity != null) && !activity.isFinishing())
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayerButton.setColorFilter(0);    // return to default color
                    }
                });
        }

        @Override
        public boolean onFrameAvailable(long presentationTimeUs) {
            return false;
        }
    };

    private final void prepareSampleMovie(File path) throws IOException {
/*
        final Activity activity = getActivity();
        if (!path.exists()) {
            final BufferedInputStream in = new BufferedInputStream(activity.getResources().openRawResource(R.raw.test));
            final BufferedOutputStream out = new BufferedOutputStream(activity.openFileOutput(path.getName(), Context.MODE_PRIVATE));
            byte[] buf = new byte[8192];
            int size = in.read(buf);
            while (size > 0) {
                out.write(buf, 0, size);
                size = in.read(buf);
            }
            in.close();
            out.flush();
            out.close();
        }
*/
    }

    private void onTranscodeFinished(boolean isSuccess, String toastMessage) {
        Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_LONG).show();
    }

}
