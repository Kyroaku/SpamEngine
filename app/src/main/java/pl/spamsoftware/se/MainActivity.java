package pl.spamsoftware.se;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.opengl.*;
import javax.microedition.khronos.opengles.*;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.egl.EGLConfig;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity
{
	Context context;
	Spam se;
	Spam.Shader shader;
	float[] projectionMatrix, viewMatrix, vp;
	Spam.Texture texBricksD, texBricksN;
	Spam.Scene scene;

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

		context = this;

		se = new Spam(this);
		se.setRenderer(new Renderer());

        setContentView(se.getView());
    }

	private class Renderer implements GLSurfaceView.Renderer
	{
		@Override
		public void onSurfaceCreated(GL10 p1, EGLConfig p2)
		{
			se.glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
			se.glClearDepthf(1.0f);

			se.glEnable(se.GL_DEPTH_TEST);
			se.glEnable(se.GL_CULL_FACE);
			se.glFrontFace(se.GL_CCW);

			//se.glEnable(se.GL_TEXTURE_2D);

			shader = se.new Shader(R.raw.ambient_vs, R.raw.ambient_fs);
			if(shader.getError() != 0)
				message(shader.getErrorLog());

			projectionMatrix = new float[16];
			viewMatrix = new float[16];
			vp = new float[16];
			Matrix.setIdentityM(projectionMatrix, 0);
			Matrix.setIdentityM(viewMatrix, 0);
			Matrix.translateM(viewMatrix, 0, 0.f, 0.f, -3.f);

			texBricksD = se.new Texture(R.drawable.bricks_d);
			texBricksN = se.new Texture(R.drawable.bricks_n);

			Spam.MeshBuilder meshBuilder = se.new MeshBuilder();

			scene = se.new Scene();
			Spam.Mesh mesh = se.new Mesh();
			final int numVerts = 9000;
			float verts[] = new float[numVerts*3];
			float colors[] = new float[numVerts*3];
			Random rand = new Random();
			for(int i = 0; i < numVerts*3; i++) {
				verts[i] = (rand.nextFloat()-0.5f)*1.5f;
				colors[i] = rand.nextFloat();
			}
			mesh.setPositions(verts);
			mesh.setColors(colors);
			mesh.createVbo();
			scene.addMesh(mesh);
			Spam.Node node = se.new Node();
			node.addMesh(0);
			scene.rootNode.addChildren(node);
		}

		@Override
		public void onSurfaceChanged(GL10 p1, int w, int h)
		{
			se.glViewport(0, 0, w, h);
			float ratio = (float)w/(float)h;
			Matrix.perspectiveM(projectionMatrix, 0, 45.f, ratio, .0001f, 1000.f);
		}

		public void onUpdateFrame(float dt)
		{
			Matrix.rotateM(viewMatrix, 0, 90.0f*se.getDeltaTime(), 0f, 0f, 1f);
		}

		@Override
		public void onDrawFrame(GL10 p1)
		{
			onUpdateFrame(se.getDeltaTime());

			se.glClear(se.GL_COLOR_BUFFER_BIT | se.GL_DEPTH_BUFFER_BIT);

			shader.use();
			shader.uniform1i("uTextureNormal", 1);
			shader.uniformMatrix4fv("uProjectionMatrix", projectionMatrix);
			shader.uniformMatrix4fv("uViewMatrix", viewMatrix);
			Matrix.multiplyMM(vp, 0, projectionMatrix, 0, viewMatrix, 0);
			shader.uniformMatrix4fv("uVPMatrix", vp);

			//texBricksD.bind(0);
			//texBricksN.bind(1);
			//for(int i = 0; i < 100; i++)
			se.render(scene, shader);

			se.endFrame();
		}
	};

	private void message(final String str)
	{
		runOnUiThread(new Runnable() {
				public void run()
				{
					Toast.makeText(context, str, Toast.LENGTH_LONG).show();
				}
			});
	}
};
