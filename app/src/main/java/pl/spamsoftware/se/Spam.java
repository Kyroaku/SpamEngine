package pl.spamsoftware.se;
import android.opengl.*;
import android.content.*;
import java.nio.*;
import java.io.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.*;
import android.widget.*;
import java.util.*;
import android.view.*;
import android.view.View.*;
import java.lang.reflect.*;

public class Spam extends GLES20
{
	private Context					context;
	private GLSurfaceView			view;
	private Fps 					fps;

	public Spam(Context c)
	{
		this.context = c;
		view = new GLSurfaceView(c);
		view.setEGLContextClientVersion(2);

		fps = new Fps();
	}

	public GLSurfaceView getView() {
		return view;
	}
	public void setRenderer(GLSurfaceView.Renderer renderer) {
		view.setRenderer(renderer);
	}
	public void setTouchListener(OnTouchListener listener) {
		view.setOnTouchListener(listener);
	}
	public void requestRender() {
		view.requestRender();
	}

	public void endFrame()
	{
		if(fps.previousFrameTime < 0)
			fps.previousFrameTime = System.currentTimeMillis();
		fps.deltaTime = (System.currentTimeMillis() - fps.previousFrameTime) / 1000f;
		fps.previousFrameTime = System.currentTimeMillis();
		fps.update(fps.deltaTime);
		//view.requestRender();
	}
	public float getDeltaTime() { return fps.deltaTime; }
	public int getFps() { return fps.count; }

	public short[] getPixel(int x, int y)
	{
		short rgb[] = new short[4];
		ByteBuffer rgbBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
		glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, rgbBuffer);
		for(int i=0; i<4; i++) {
			rgb[i] = rgbBuffer.get(i)<0 ? (short)(rgbBuffer.get(i)+255) : rgbBuffer.get(i);
		}
		return rgb;
	}

	public void render(Mesh mesh)
	{
		if(mesh.hasPositions) {
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, mesh.positions);
		}
		if(mesh.hasColors) {
			glEnableVertexAttribArray(1);
			glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, mesh.colors);
		}
		if(mesh.hasTextureCoords) {
			glEnableVertexAttribArray(2);
			glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, mesh.textureCoords);
		}
		if(mesh.hasNormals) {
			glEnableVertexAttribArray(3);
			glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, mesh.normals);
		}
		if(mesh.hasTangents) {
			glEnableVertexAttribArray(4);
			glVertexAttribPointer(4, 3, GL_FLOAT, false, 0, mesh.tangents);
		}
		if(mesh.hasBitangents) {
			glEnableVertexAttribArray(5);
			glVertexAttribPointer(5, 3, GL_FLOAT, false, 0, mesh.bitangents);
		}
		if(mesh.hasBones) {
			glEnableVertexAttribArray(6);
			glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, mesh.boneIds);
			glEnableVertexAttribArray(7);
			glVertexAttribPointer(7, 4, GL_FLOAT, false, 0, mesh.boneWeights);
		}

		if(mesh.hasIndices)
			glDrawElements(GL_TRIANGLES, mesh.numIndices, GL_UNSIGNED_SHORT, mesh.indices);
		else
			glDrawArrays(GL_TRIANGLES, 0, mesh.numPositions);
	}
	public void renderVbo(Mesh mesh)
	{
		if(mesh.hasPositions) {
			glBindBuffer(GL_ARRAY_BUFFER, mesh.vbo[0]);
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 12, 0);
		}

		if(mesh.hasIndices) {
			glBindBuffer(GL_ARRAY_BUFFER, mesh.vbo[0]);
			glDrawElements(GL_TRIANGLES, mesh.numIndices, GL_UNSIGNED_SHORT, 0);
		}
		else
			glDrawArrays(GL_TRIANGLES, 0, mesh.numPositions);
	}

	public void render(Scene scene, Node node, Shader shader, mat4 transformation)
	{
		mat4 absoluteMatrix = transformation.multiply(node.transformation);

		shader.uniformMatrix4fv("se_ModelMatrix", node.transformation.get());
		for(int i = 0; i < node.meshes.size(); i++)
		{
			renderVbo(scene.meshes.get(node.meshes.get(i)));
		}

		for(int i = 0; i < node.childrens.size(); i++)
			render(scene, node.childrens.get(i), shader, absoluteMatrix);
	}
	public void render(Scene scene, Shader shader)
	{
		for(int i=0; i<scene.bones.size(); i++)
			shader.uniformMatrix4fv("se_BoneMatrix["+i+"]", scene.bones.get(i).get());
		render(scene, scene.rootNode, shader, new mat4(1.f));
	}

	// =============================== Fps
	private class Fps
	{
		float time;
		int count, frames;
		long previousFrameTime;
		float deltaTime;

		public Fps()
		{
			time = 0.0f;
			count = frames = 0;
			previousFrameTime = -1;
			deltaTime= 0.0f;
		}

		public void update(float dt)
		{
			time += dt;
			frames++;
			if(time >= 1.0f) {
				time-=0.0f;
				count = frames;
				frames = 0;
			}
		}
	}

	// =============================== Scene
	public class Scene
	{
		ArrayList<Mesh> meshes;
		ArrayList<Texture> textures;
		ArrayList<mat4> bones;
		Node rootNode;

		public Scene()
		{
			meshes = new ArrayList<Mesh>();
			textures = new ArrayList<Texture>();
			bones = new ArrayList<mat4>();
			rootNode = new Node();
		}
		public void addMesh(Mesh m) { meshes.add(m); }
		public void addTexture(Texture t) { textures.add(t); }
		public void addBone(mat4 b) { bones.add(b); }
		public Node getRootNode() { return rootNode; }
	}

	// =============================== Mesh
	public class Mesh
	{
		FloatBuffer positions, textureCoords, colors, normals,
		tangents, bitangents, boneWeights, boneIds;
		ShortBuffer indices;
		boolean hasPositions, hasTextureCoords, hasColors, hasNormals,
		hasTangents, hasBitangents, hasIndices, hasBones;
		int numPositions, numIndices;
		int vbo[];

		public Mesh()
		{
			hasPositions = false;
			hasTextureCoords = false;
			hasColors = false;
			hasNormals = false;
			hasTangents = false;
			hasBitangents = false;
			hasBones = false;
			numPositions = 0;
			numIndices = 0;
		}
		public void setPositions(float v[])
		{
			positions = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			positions.put(v);
			positions.position(0);
			hasPositions = true;
			numPositions = v.length / 3;
		}
		public void setTextureCoords(float v[])
		{
			textureCoords = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			textureCoords.put(v);
			textureCoords.position(0);
			hasTextureCoords = true;
		}
		public void setColors(float v[])
		{
			colors = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			colors.put(v);
			colors.position(0);
			hasColors = true;
		}
		public void setNormals(float v[])
		{
			normals = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			normals.put(v);
			normals.position(0);
			hasNormals = true;
		}
		public void setTangents(float v[])
		{
			tangents = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			tangents.put(v);
			tangents.position(0);
			hasTangents = true;
		}
		public void setBitangents(float v[])
		{
			bitangents = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			bitangents.put(v);
			bitangents.position(0);
			hasBitangents = true;
		}
		public void setIndices(short v[])
		{
			indices = ByteBuffer.allocateDirect(v.length * 2).
				order(ByteOrder.nativeOrder()).asShortBuffer();
			indices.put(v);
			indices.position(0);
			hasIndices = true;
			numIndices = v.length;
		}
		public void setBoneWeights(float v[])
		{
			boneWeights = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			boneWeights.put(v);
			boneWeights.position(0);
			hasBones = true;
		}
		public void setBoneIds(float v[])
		{
			boneIds = ByteBuffer.allocateDirect(v.length * 4).
				order(ByteOrder.nativeOrder()).asFloatBuffer();
			boneIds.put(v);
			boneIds.position(0);
			hasBones = true;
		}
		public void createVbo()
		{
			vbo = new int[1];
			glGenBuffers(1, vbo, 0);
			glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
			glBufferData(GL_ARRAY_BUFFER, positions.capacity()*4, positions, GL_STATIC_DRAW);
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
	}

	// =============================== Node
	public class Node
	{
		ArrayList<Integer> meshes;
		ArrayList<Node> childrens;
		Node parent;
		mat4 transformation;

		public Node()
		{
			meshes = new ArrayList<Integer>();
			childrens = new ArrayList<Node>();
			parent = null;
			transformation = new mat4(1.f);
		}
		public void addChildren(Node n) {
			n.parent = this;
			childrens.add(n);
		}
		public void addMesh(int m) {
			meshes.add(m);
		}
		public Node getChildren(int i) {
			return childrens.get(i);
		}
		public int getMesh(int i) {
			return meshes.get(i);
		}
	}

	// =============================== Light
	public class Light
	{
		float[] position, ambient, diffuse, specular;
		boolean enabled;

		public Light()
		{
			position = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
			ambient = new float[] { 0.1f, 0.1f, 0.1f };
			diffuse = new float[] { 1.0f, 1.0f, 1.0f };
			specular = new float[] { 1.0f, 1.0f, 1.0f };
			enabled = true;
		}

		public void disable() { enabled = false; }
		public void enable() { enabled = true; }
		public boolean isEnabled() { return enabled; }

		public void setPosition(float a[]) {
			position = a;
		}
		public void setPosition(float x, float y, float z, float w) {
			position[0] = x; position[1] = y; position[2] = z; position[3] = w;
		}
		public void setAmbient(float a[]) {
			ambient = a;
		}
		public void setAmbient(float r, float g, float b) {
			ambient[0] = r; ambient[1] = g; ambient[2] = b;
		}
		public void setDiffuse(float a[]) {
			diffuse = a;
		}
		public void setDiffuse(float r, float g, float b) {
			diffuse[0] = r; diffuse[1] = g; diffuse[2] = b;
		}
		public void setSpecular(float a[]) {
			specular = a;
		}
		public void setSpecular(float r, float g, float b) {
			specular[0] = r; specular[1] = g; specular[2] = b;
		}
		public float[] getPosition() { return position; }
		public float[] getAmbient() { return ambient; }
		public float[] getDiffuse() { return diffuse; }
		public float[] getSpecular() { return specular; }
	}

	// =============================== Shader
	public class Shader
	{
		private int program;
		private int error;
		private String errorLog;

		public Shader()
		{
			program = 0;
			errorLog = "";
			error = 0;
		}

		public Shader(String vertexShaderSrc, String fragmentShaderSrc)
		{
			program = 0;
			errorLog = "";
			error = 0;

			setup(vertexShaderSrc, fragmentShaderSrc);
		}

		public Shader(int vertexShaderSrc, int fragmentShaderSrc)
		{
			program = 0;
			errorLog = "";
			error = 0;

			InputStream is = context.getResources().openRawResource(vertexShaderSrc);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			String vs = "", fs = "";
			do
			{
				try { line = br.readLine(); }
				catch (IOException e) {}
				if(line == null)
					break;
				vs += line;
			} while(true);

			is = context.getResources().openRawResource(fragmentShaderSrc);
			br = new BufferedReader(new InputStreamReader(is));
			do
			{
				try { line = br.readLine(); }
				catch (IOException e) {}
				if(line == null)
					break;
				fs += line;
			} while(true);

			setup(vs, fs);
		}

		public void setup(String vertexShaderSrc, String fragmentShaderSrc)
		{
			int vertexShader, fragmentShader;
			int status[] = new int[1];

			vertexShader = glCreateShader(GL_VERTEX_SHADER);
			glShaderSource(vertexShader, vertexShaderSrc);
			glCompileShader(vertexShader);
			glGetShaderiv(vertexShader, GL_COMPILE_STATUS, status, 0);
			if(status[0] == 0) {
				error = 1;
				errorLog += "Vertex Shader log:\n"+glGetShaderInfoLog(vertexShader);
			}

			fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
			glShaderSource(fragmentShader, fragmentShaderSrc);
			glCompileShader(fragmentShader);
			glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, status, 0);
			if(status[0] == 0) {
				error = 2;
				errorLog += "\nFragment Shader log:\n"+glGetShaderInfoLog(fragmentShader);
			}

			program = glCreateProgram();
			glAttachShader(program, vertexShader);
			glAttachShader(program, fragmentShader);

			glBindAttribLocation(program, 0, "se_Position");
			glBindAttribLocation(program, 1, "se_Color");
			glBindAttribLocation(program, 2, "se_TextureCoord");
			glBindAttribLocation(program, 3, "se_Normal");
			glBindAttribLocation(program, 4, "se_Tangent");
			glBindAttribLocation(program, 5, "se_Bitangent");
			glBindAttribLocation(program, 6, "se_BoneId");
			glBindAttribLocation(program, 7, "se_BoneWeight");

			glLinkProgram(program);
			glGetProgramiv(program, GL_LINK_STATUS, status, 0);
			if(status[0] == 0) {
				error = 3;
				errorLog += "\nProgram Shader log:\n"+glGetProgramInfoLog(program);
				return;
			}
		}

		public void use()			{ glUseProgram(program); }
		public int getProgram()		{ return program; }
		public int getError()		{ return error; }
		public String getErrorLog()	{ return errorLog; }

		public void uniform1f(String loc, float v) { glUniform1f(glGetUniformLocation(program, loc), v); }
		public void uniform2f(String loc, float v1, float v2) { glUniform2f(glGetUniformLocation(program, loc), v1, v2); }
		public void uniform3f(String loc, float v1, float v2, float v3) { glUniform3f(glGetUniformLocation(program, loc), v1, v2, v3); }
		public void uniform4f(String loc, float v1, float v2, float v3, float v4) { glUniform4f(glGetUniformLocation(program, loc), v1, v2, v3, v4); }
		public void uniform1fv(String loc, float[] v) { glUniform1fv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniform2fv(String loc, float[] v) { glUniform2fv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniform3fv(String loc, float[] v) { glUniform3fv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniform4fv(String loc, float[] v) { glUniform4fv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniformMatrix2fv(String loc, float v[]) { glUniformMatrix2fv(glGetUniformLocation(program, loc), 1, false, v, 0); }
		public void uniformMatrix3fv(String loc, float v[]) { glUniformMatrix3fv(glGetUniformLocation(program, loc), 1, false, v, 0); }
		public void uniformMatrix4fv(String loc, float v[]) { glUniformMatrix4fv(glGetUniformLocation(program, loc), 1, false, v, 0); }
		public void uniform1i(String loc, int v) { glUniform1i(glGetUniformLocation(program, loc), v); }
		public void uniform2i(String loc, int v1, int v2) { glUniform2i(glGetUniformLocation(program, loc), v1, v2); }
		public void uniform3i(String loc, int v1, int v2, int v3) { glUniform3i(glGetUniformLocation(program, loc), v1, v2, v3); }
		public void uniform4i(String loc, int v1, int v2, int v3, int v4) { glUniform4i(glGetUniformLocation(program, loc), v1, v2, v3, v4); }
		public void uniform1iv(String loc, int v[]) { glUniform1iv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniform2iv(String loc, int v[]) { glUniform2iv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniform3iv(String loc, int v[]) { glUniform3iv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniform4iv(String loc, int v[]) { glUniform4iv(glGetUniformLocation(program, loc), 1, v, 0); }
		public void uniformLightsNumber(int n) { uniform1i("u_LightsNumber", n); }
		public void uniformLight(int id, Light l)
		{
			uniform4fv("u_Lights["+id+"].position", l.getPosition());
			uniform3fv("u_Lights["+id+"].ambient", l.getAmbient());
			uniform3fv("u_Lights["+id+"].diffuse", l.getDiffuse());
			uniform3fv("u_Lights["+id+"].specular", l.getSpecular());
			uniform1i("u_Lights["+id+"].enabled", l.isEnabled()?1:0);
		}
	};

	// =============================== Texture
	public class Texture
	{
		private int texture;
		private Bitmap bitmap;
		private float sizeDiff;

		public Texture(int res)
		{
			int t[] = new int[1];
			bitmap = BitmapFactory.decodeStream(context.getResources().openRawResource(res));
			glGenTextures(1, t, 0);
			glBindTexture(GL_TEXTURE_2D, t[0]);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT);
			GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
			texture = t[0];
			sizeDiff = (float)bitmap.getWidth()/(float)bitmap.getHeight();
			bitmap.recycle();
		}

		public void bind(int i) {
			glActiveTexture(GL_TEXTURE0 + i);
			glBindTexture(GL_TEXTURE_2D, texture);
		}
	};

	// =============================== MeshBuilder
	public class MeshBuilder
	{
		public Mesh rect(float w, float h, int sx, int sy)
		{
			Mesh m = new Mesh();
			float verts[] = new float[(sx+1)*(sy+1)*3];
			float texCoords[] = new float[(sx+1)*(sy+1)*3];
			float colors[] = new float[(sx+1)*(sy+1)*4];
			float normals[] = new float[(sx+1)*(sy+1)*3];
			float tangents[] = new float[(sx+1)*(sy+1)*3];
			float bitangents[] = new float[(sx+1)*(sy+1)*3];
			short indices[] = new short[sx*sy*6];
			for(int y = 0; y < sy+1; y++)
			{
				for(int x = 0; x < sx+1; x++)
				{
					verts[(y*(sx+1)+x)*3+0] = x * (w / sx) - w/2.f;
					verts[(y*(sx+1)+x)*3+1] = y * (h / sy) - h/2.f;
					verts[(y*(sx+1)+x)*3+2] = 0.f;
					texCoords[(y*(sx+1)+x)*3+0] = x * (1.f/sx);
					texCoords[(y*(sx+1)+x)*3+1] = y * (1.f/sy);
					texCoords[(y*(sx+1)+x)*3+2] = 0.f;
				}
			}
			for(int i = 0; i < colors.length; i++)
				colors[i] = 1.f;
			for(int i = 0; i < normals.length; i+=3)
			{
				tangents[i+0] = 1.f; tangents[i+1] = 0.f; tangents[i+2] = 0.f; 
				bitangents[i+0] = 0.f; bitangents[i+1] = 1.f; bitangents[i+2] = 0.f; 
				normals[i+0] = 0.f; normals[i+1] = 0.f; normals[i+2] = 1.f; 
			}
			for(int y = 0; y < sy; y++)
			{
				for(int x = 0; x < sx; x++)
				{
					indices[6*(y*sx+x)+0] = (short)((y+0)*(sx+1) + x + 0);
					indices[6*(y*sx+x)+1] = (short)((y+0)*(sx+1) + x + 1);
					indices[6*(y*sx+x)+2] = (short)((y+1)*(sx+1) + x + 1);
					indices[6*(y*sx+x)+3] = (short)((y+1)*(sx+1) + x + 1);
					indices[6*(y*sx+x)+4] = (short)((y+1)*(sx+1) + x + 0);
					indices[6*(y*sx+x)+5] = (short)((y+0)*(sx+1) + x + 0);
				}
			}
			m.setPositions(verts);
			m.setColors(colors);
			m.setTextureCoords(texCoords);
			m.setNormals(normals);
			m.setTangents(tangents);
			m.setBitangents(bitangents);
			m.setIndices(indices);

			return m;
		}

		public Mesh box(float w, float h, float d, int sx, int sy, int sz)
		{
			Mesh m = new Mesh();
			m.numPositions = (sx+1)*(sy+1)*2 + (sy+1)*(sz+1)*2 + (sx+1)*(sz+1)*2;
			m.numIndices = sx * sy * 12 + sz * sy * 12 + sx * sz * 12;
			float verts[] = new float[m.numPositions*3];
			float texCoords[] = new float[m.numPositions*3];
			float colors[] = new float[m.numPositions*4];
			float normals[] = new float[m.numPositions*3];
			float tangents[] = new float[m.numPositions*3];
			float bitangents[] = new float[m.numPositions*3];
			short indices[] = new short[m.numIndices];

			float boneIds[] = new float[m.numPositions*4];
			float boneWeights[] = new float[m.numPositions*4];

			for(int y = 0; y < sy+1; y++)
			{
				for(int x = 0; x < sx+1; x++)
				{
					verts[(y*(sx+1)+x)*3+0] = x * (w / sx) - w/2.f;
					verts[(y*(sx+1)+x)*3+1] = y * (h / sy) - h/2.f;
					verts[(y*(sx+1)+x)*3+2] = d/2.f;
					texCoords[(y*(sx+1)+x)*3+0] = x * (1.f/sx);
					texCoords[(y*(sx+1)+x)*3+1] = y * (1.f/sy);
					texCoords[(y*(sx+1)+x)*3+2] = 0.f;
					verts[(y*(sx+1)+x)*3+0 + (sx+1)*(sy+1)*3] = -x * (w / sx) + w/2.f;
					verts[(y*(sx+1)+x)*3+1 + (sx+1)*(sy+1)*3] = y * (h / sy) - h/2.f;
					verts[(y*(sx+1)+x)*3+2 + (sx+1)*(sy+1)*3] = -d/2.f;
					texCoords[(y*(sx+1)+x)*3+0 + (sx+1)*(sy+1)*3] = x * (1.f/sx);
					texCoords[(y*(sx+1)+x)*3+1 + (sx+1)*(sy+1)*3] = y * (1.f/sy);
					texCoords[(y*(sx+1)+x)*3+2 + (sx+1)*(sy+1)*3] = 0.f;
				}
			}
			for(int y = 0; y < sy+1; y++)
			{
				for(int z = 0; z < sz+1; z++)
				{
					verts[(y*(sz+1)+z)*3+2 + (sx+1)*(sy+1)*6] = -z * (d / sz) + d/2.f;
					verts[(y*(sz+1)+z)*3+1 + (sx+1)*(sy+1)*6] = y * (h / sy) - h/2.f;
					verts[(y*(sz+1)+z)*3+0 + (sx+1)*(sy+1)*6] = w/2.f;
					texCoords[(y*(sz+1)+z)*3+2 + (sx+1)*(sy+1)*6] = 0.f;
					texCoords[(y*(sz+1)+z)*3+1 + (sx+1)*(sy+1)*6] = y * (1.f / sy);
					texCoords[(y*(sz+1)+z)*3+0 + (sx+1)*(sy+1)*6] = z * (1.f / sz);
					verts[(y*(sz+1)+z)*3+2 + (sz+1)*(sy+1)*3 + (sx+1)*(sy+1)*6] = z * (d / sz) - d/2.f;
					verts[(y*(sz+1)+z)*3+1 + (sz+1)*(sy+1)*3 + (sx+1)*(sy+1)*6] = y * (h / sy) - h/2.f;
					verts[(y*(sz+1)+z)*3+0 + (sz+1)*(sy+1)*3 + (sx+1)*(sy+1)*6] = -w/2.f;
					texCoords[(y*(sz+1)+z)*3+2 + (sz+1)*(sy+1)*3 + (sx+1)*(sy+1)*6] = 0.f;
					texCoords[(y*(sz+1)+z)*3+1 + (sz+1)*(sy+1)*3 + (sx+1)*(sy+1)*6] = y * (1.f / sy);
					texCoords[(y*(sz+1)+z)*3+0 + (sz+1)*(sy+1)*3 + (sx+1)*(sy+1)*6] = z * (1.f / sz);
				}
			}
			for(int z = 0; z < sz+1; z++)
			{
				for(int x = 0; x < sx+1; x++)
				{
					verts[(z*(sx+1)+x)*3+2 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = -z * (d / sz) + d/2.f;
					verts[(z*(sx+1)+x)*3+0 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = x * (w / sx) - w/2.f;
					verts[(z*(sx+1)+x)*3+1 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = h/2.f;
					texCoords[(z*(sx+1)+x)*3+2 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = 0.f;
					texCoords[(z*(sx+1)+x)*3+0 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = x * (1.f / sx);
					texCoords[(z*(sx+1)+x)*3+1 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = z * (1.f / sz);
					verts[(z*(sx+1)+x)*3+2 + (sz+1)*(sx+1)*3 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = z * (d / sz) - d/2.f;
					verts[(z*(sx+1)+x)*3+0 + (sz+1)*(sx+1)*3 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = x * (w / sx) - w/2.f;
					verts[(z*(sx+1)+x)*3+1 + (sz+1)*(sx+1)*3 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = -h/2.f;
					texCoords[(z*(sx+1)+x)*3+2 + (sz+1)*(sx+1)*3 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = 0.f;
					texCoords[(z*(sx+1)+x)*3+0 + (sz+1)*(sx+1)*3 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = x * (1.f / sx);
					texCoords[(z*(sx+1)+x)*3+1 + (sz+1)*(sx+1)*3 + (sx+1)*(sy+1)*6 + (sz+1)*(sy+1)*6] = z * (1.f / sz);
				}
			}
			for(int i = 0; i < colors.length; i++)
				colors[i] = 1.f;
			for(int i = 0; i < m.numPositions*3; i+=3)
			{
				if(i < (sx+1)*(sy+1)*3) {
					tangents[i+0] = 1.f; tangents[i+1] = 0.f; tangents[i+2] = 0.f; 
					bitangents[i+0] = 0.f; bitangents[i+1] = 1.f; bitangents[i+2] = 0.f; 
					normals[i+0] = 0.f; normals[i+1] = 0.f; normals[i+2] = 1.f; 
				}
				else if(i < (sx+1)*(sy+1)*6) {
					tangents[i+0] = -1.f; tangents[i+1] = 0.f; tangents[i+2] = 0.f; 
					bitangents[i+0] = 0.f; bitangents[i+1] = 1.f; bitangents[i+2] = 0.f; 
					normals[i+0] = 0.f; normals[i+1] = 0.f; normals[i+2] = -1.f; 
				}
				else if(i < (sx+1)*(sy+1)*6 + (sy+1)*(sz+1)*3) {
					tangents[i+0] = 0.f; tangents[i+1] = 0.f; tangents[i+2] = -1.f; 
					bitangents[i+0] = 0.f; bitangents[i+1] = 1.f; bitangents[i+2] = 0.f; 
					normals[i+0] = 1.f; normals[i+1] = 0.f; normals[i+2] = 0.f; 
				}
				else if(i < (sx+1)*(sy+1)*6 + (sy+1)*(sz+1)*6) {
					tangents[i+0] = 0.f; tangents[i+1] = 0.f; tangents[i+2] = 1.f; 
					bitangents[i+0] = 0.f; bitangents[i+1] = 1.f; bitangents[i+2] = 0.f; 
					normals[i+0] = -1.f; normals[i+1] = 0.f; normals[i+2] = 0.f; 
				}
				else if(i < (sx+1)*(sy+1)*6 + (sy+1)*(sz+1)*6 + (sx+1)*(sz+1)*3) {
					tangents[i+0] = 1.f; tangents[i+1] = 0.f; tangents[i+2] = 0.f; 
					bitangents[i+0] = 0.f; bitangents[i+1] = 0.f; bitangents[i+2] = -1.f; 
					normals[i+0] = 0.f; normals[i+1] = 1.f; normals[i+2] = 0.f; 
				}
				else {
					tangents[i+0] = 1.f; tangents[i+1] = 0.f; tangents[i+2] = 0.f; 
					bitangents[i+0] = 0.f; bitangents[i+1] = 0.f; bitangents[i+2] = 1.f; 
					normals[i+0] = 0.f; normals[i+1] = -1.f; normals[i+2] = 0.f; 
				}
			}
			for(int i = 0; i < 2; i++)
			{
				for(int y = 0; y < sy; y++)
				{
					for(int x = 0; x < sx; x++)
					{
						indices[i*6*(sx*sy) + 6*(y*sx+x)+0] = (short)((y+0)*(sx+1) + x + 0 + i*(sx+1)*(sy+1));
						indices[i*6*(sx*sy) + 6*(y*sx+x)+1] = (short)((y+0)*(sx+1) + x + 1 + i*(sx+1)*(sy+1));
						indices[i*6*(sx*sy) + 6*(y*sx+x)+2] = (short)((y+1)*(sx+1) + x + 1 + i*(sx+1)*(sy+1));
						indices[i*6*(sx*sy) + 6*(y*sx+x)+3] = (short)((y+1)*(sx+1) + x + 1 + i*(sx+1)*(sy+1));
						indices[i*6*(sx*sy) + 6*(y*sx+x)+4] = (short)((y+1)*(sx+1) + x + 0 + i*(sx+1)*(sy+1));
						indices[i*6*(sx*sy) + 6*(y*sx+x)+5] = (short)((y+0)*(sx+1) + x + 0 + i*(sx+1)*(sy+1));
					}
				}
			}
			for(int i = 0; i < 2; i++)
			{
				for(int y = 0; y < sy; y++)
				{
					for(int z = 0; z < sz; z++)
					{
						indices[i*6*(sz*sy) + 6*(y*sz+z)+0 + sx * sy * 12] = (short)((y+0)*(sz+1) + z + 0 + i*(sz+1)*(sy+1) + (sx+1)*(sy+1)*2);
						indices[i*6*(sz*sy) + 6*(y*sz+z)+1 + sx * sy * 12] = (short)((y+0)*(sz+1) + z + 1 + i*(sz+1)*(sy+1) + (sx+1)*(sy+1)*2);
						indices[i*6*(sz*sy) + 6*(y*sz+z)+2 + sx * sy * 12] = (short)((y+1)*(sz+1) + z + 1 + i*(sz+1)*(sy+1) + (sx+1)*(sy+1)*2);
						indices[i*6*(sz*sy) + 6*(y*sz+z)+3 + sx * sy * 12] = (short)((y+1)*(sz+1) + z + 1 + i*(sz+1)*(sy+1) + (sx+1)*(sy+1)*2);
						indices[i*6*(sz*sy) + 6*(y*sz+z)+4 + sx * sy * 12] = (short)((y+1)*(sz+1) + z + 0 + i*(sz+1)*(sy+1) + (sx+1)*(sy+1)*2);
						indices[i*6*(sz*sy) + 6*(y*sz+z)+5 + sx * sy * 12] = (short)((y+0)*(sz+1) + z + 0 + i*(sz+1)*(sy+1) + (sx+1)*(sy+1)*2);
					}
				}
			}
			for(int i = 0; i < 2; i++)
			{
				for(int x = 0; x < sx; x++)
				{
					for(int z = 0; z < sz; z++)
					{
						indices[i*6*(sz*sx) + 6*(x*sz+z)+0 + sx * sy * 12 + sz * sy * 12] = (short)((x+0)*(sz+1) + z + 0 + i*(sz+1)*(sx+1) + (sx+1)*(sy+1)*2 + (sx+1)*(sz+1)*2);
						indices[i*6*(sz*sx) + 6*(x*sz+z)+1 + sx * sy * 12 + sz * sy * 12] = (short)((x+0)*(sz+1) + z + 1 + i*(sz+1)*(sx+1) + (sx+1)*(sy+1)*2 + (sx+1)*(sz+1)*2);
						indices[i*6*(sz*sx) + 6*(x*sz+z)+2 + sx * sy * 12 + sz * sy * 12] = (short)((x+1)*(sz+1) + z + 1 + i*(sz+1)*(sx+1) + (sx+1)*(sy+1)*2 + (sx+1)*(sz+1)*2);
						indices[i*6*(sz*sx) + 6*(x*sz+z)+3 + sx * sy * 12 + sz * sy * 12] = (short)((x+1)*(sz+1) + z + 1 + i*(sz+1)*(sx+1) + (sx+1)*(sy+1)*2 + (sx+1)*(sz+1)*2);
						indices[i*6*(sz*sx) + 6*(x*sz+z)+4 + sx * sy * 12 + sz * sy * 12] = (short)((x+1)*(sz+1) + z + 0 + i*(sz+1)*(sx+1) + (sx+1)*(sy+1)*2 + (sx+1)*(sz+1)*2);
						indices[i*6*(sz*sx) + 6*(x*sz+z)+5 + sx * sy * 12 + sz * sy * 12] = (short)((x+0)*(sz+1) + z + 0 + i*(sz+1)*(sx+1) + (sx+1)*(sy+1)*2 + (sx+1)*(sz+1)*2);
					}
				}
			}

			for(int i=0; i<m.numPositions; i+=4)
			{
				boneIds[i+0] = 0;
				boneIds[i+1] = 0;
				boneIds[i+2] = 0;
				boneIds[i+3] = 0;
				boneWeights[i+0] = 1.0f;
				boneWeights[i+1] = 0.0f;
				boneWeights[i+2] = 0.0f;
				boneWeights[i+3] = 0.0f;
			}

			m.setPositions(verts);
			m.setColors(colors);
			m.setTextureCoords(texCoords);
			m.setNormals(normals);
			m.setTangents(tangents);
			m.setBitangents(bitangents);
			m.setIndices(indices);

			m.setBoneIds(boneIds);
			m.setBoneWeights(boneWeights);

			return m;
		}
	};

	// =============================== vec array
	public class vec2Array
	{

	}

	// =============================== vec
	public class vec2
	{
		public float x, y;
		public vec2() { set(0.f, 0.f); }
		public vec2(float x, float y) { set(x, y); }
		public void set(float x, float y) { this.x = x; this.y = y; }
		public float[] get() { return new float[] { x, y }; }
		public vec2 add(vec2 v) { return new vec2(x+v.x, y+v.y); }
		public vec2 subtract(vec2 v) { return new vec2(x-v.x, y-v.y); }
		public float dot(vec2 v) { return x*v.x + y*v.y; }
	}

	public class vec3
	{
		public float x, y, z;
		public vec3() { set(0.f, 0.f, 0.f); }
		public vec3(float x, float y, float z) { set(x, y, z); }
		public void set(float x, float y, float z) {
			this.x = x; this.y = y; this.z = z;
		}
		public float[] get() { return new float[] { x, y, z }; }
		public vec3 add(vec3 v) { return new vec3(x+v.x, y+v.y, z+v.z); }
		public vec3 subtract(vec3 v) { return new vec3(x-v.x, y-v.y, z-v.z); }
		public float dot(vec3 v) { return x*v.x + y*v.y + z*v.z; }
		public vec3 cross(vec3 v) {
			return new vec3( (y-v.z)*(z-v.y), (z-v.x)*(x-v.z), (x-v.y)*(y-v.x) );
		}
	}

	public class vec4
	{
		public float x, y, z, w;
		public vec4() { set(0.f, 0.f, 0.f, 1.f); }
		public vec4(float x, float y, float z, float w) { set(x, y, z, w); }
		public vec4(vec2 v, float z, float w) { set(v.x, v.y, z, w); }
		public vec4(vec3 v, float w) { set(v.x, v.y, v.z, w); }
		public void set(float x, float y, float z, float w) {
			this.x = x; this.y = y; this.z = z; this.w = w;
		}
		public float[] get() { return new float[] { x, y, z, w }; }
		public vec4 add(vec4 v) { return new vec4(x+v.x, y+v.y, z+v.z, w+v.w); }
		public vec4 subtract(vec4 v) { return new vec4(x-v.x, y-v.y, z-v.z, w+v.w); }
		public float dot(vec4 v) { return x*v.x + y*v.y + z*v.z + w*v.w; }
	}

	// =============================== mat
	public class mat2
	{
		public float data[];
		public mat2() { set(1.f, 0.f, 0.f, 1.f); }
		public mat2(float v) { set(v, 0.f, 0.f, v); }
		public mat2(float v[]) { data = v; }
		public mat2(float a1, float a2, float b1, float b2) { set(a1, a2, b1, b2); }
		public void set(float a1, float a2, float b1, float b2) {
			data = new float[] { a1, a2, b1, b2 };
		}
		public float[] get() { return data; }
		public mat2 multiply(mat2 m) {
			return new mat2(
				data[0]*m.data[0]+data[1]*m.data[2], data[0]*m.data[1]+data[1]*m.data[3], 
				data[2]*m.data[0]+data[3]*m.data[2], data[2]*m.data[1]+data[3]*m.data[3]);
		}
		public vec2 multiply(vec2 v) {
			return new vec2(
				data[0]*v.x+data[1]*v.y, data[2]*v.x+data[3]*v.y);

		}
		public mat2 multiply(float v) {
			return new mat2(
				data[0]*v, data[1]*v, 
				data[2]*v, data[3]*v);
		}
		public mat2 transpose() {
			return new mat2(data[0], data[2], data[1], data[3]);
		}
		public mat2 inverse() {
			mat2 m = new mat2(data[3], -data[1], -data[2], data[0]);
			float det = 1.0f / (data[0]*data[3] - data[1]*data[2]);
			return m.multiply(det);
		}
	}

	public class mat3
	{
		float data[];
		public mat3() { set(1.f, 0.f, 0.f, 0.f, 1.f, 0.f, 0.f, 0.f, 1.f); }
		public mat3(float v) { set(v, 0.f, 0.f, 0.f, v, 0.f, 0.f, 0.f, v); }
		public mat3(float v[]) { data = v; }
		public mat3(float a1, float a2, float a3, float b1, float b2, float b3, float c1, float c2, float c3)
		{
			set(a1, a2, a3, b1, b2, b3, c1, c2, c3);
		}
		public void set(float a1, float a2, float a3, float b1, float b2, float b3, float c1, float c2, float c3)
		{
			data = new float[] { a1, a2, a3, b1, b2, b3, c1, c2, c3 };
		}
		public float[] get() { return data; }
		public mat3 multiply(mat3 m) {
			return new mat3(
				data[0]*m.data[0]+data[1]*m.data[3]+data[2]*m.data[6],
				data[0]*m.data[1]+data[1]*m.data[4]+data[2]*m.data[7],
				data[0]*m.data[2]+data[1]*m.data[5]+data[2]*m.data[8],

				data[3]*m.data[0]+data[4]*m.data[3]+data[5]*m.data[6],
				data[3]*m.data[1]+data[4]*m.data[4]+data[5]*m.data[7],
				data[3]*m.data[2]+data[4]*m.data[5]+data[5]*m.data[8],

				data[6]*m.data[0]+data[7]*m.data[3]+data[8]*m.data[6],
				data[6]*m.data[1]+data[7]*m.data[4]+data[8]*m.data[7],
				data[6]*m.data[2]+data[7]*m.data[5]+data[8]*m.data[8]);
		}
		public vec3 multiply(vec3 v) {
			return new vec3(
				data[0]*v.x+data[1]*v.y+data[2]*v.z,
				data[3]*v.x+data[4]*v.y+data[5]*v.z,
				data[6]*v.x+data[7]*v.y+data[8]*v.z);
		}
		public mat3 multiply(float v) {
			return new mat3(
				data[0]*v, data[1]*v, data[2]*v,
				data[3]*v, data[4]*v, data[5]*v,
				data[6]*v, data[7]*v, data[8]*v);
		}
		public mat3 transpose() {
			return new mat3(
				data[0], data[3], data[6],
				data[1], data[4], data[7],
				data[2], data[5], data[8]);
		}
	}

	public class mat4
	{
		float data[];
		public mat4() { set(1.f, 0.f, 0.f, 0.f, 0.f, 1.f, 0.f, 0.f, 0.f, 0.f, 1.f, 0.f, 0.f, 0.f, 0.f, 1.f); }
		public mat4(float v) { set(v, 0.f, 0.f, 0.f, 0.f, v, 0.f, 0.f, 0.f, 0.f, v, 0.f, 0.f, 0.f, 0.f, v); }
		public mat4(float v[]) { data = v; }
		public mat4(float a1, float a2, float a3, float a4, float b1, float b2, float b3, float b4, float c1, float c2, float c3, float c4, float d1, float d2, float d3, float d4)
		{
			set(a1, a2, a3, a4, b1, b2, b3, b4, c1, c2, c3, c4, d1, d2, d3, d4);
		}
		public void set(float a1, float a2, float a3, float a4, float b1, float b2, float b3, float b4, float c1, float c2, float c3, float c4, float d1, float d2, float d3, float d4)
		{
			data = new float[] { a1, a2, a3, a4, b1, b2, b3, b4, c1, c2, c3, c4, d1, d2, d3, d4 };
		}
		public float[] get() { return data; }
		public mat4 multiply(mat4 m) {
			return new mat4(
				data[0]*m.data[0]+data[1]*m.data[4]+data[2]*m.data[8]+data[3]*m.data[12],
				data[0]*m.data[1]+data[1]*m.data[5]+data[2]*m.data[9]+data[3]*m.data[13],
				data[0]*m.data[2]+data[1]*m.data[6]+data[2]*m.data[10]+data[3]*m.data[14],
				data[0]*m.data[3]+data[1]*m.data[7]+data[2]*m.data[11]+data[3]*m.data[15],

				data[4]*m.data[0]+data[5]*m.data[4]+data[6]*m.data[8]+data[7]*m.data[12],
				data[4]*m.data[1]+data[5]*m.data[5]+data[6]*m.data[9]+data[7]*m.data[13],
				data[4]*m.data[2]+data[5]*m.data[6]+data[6]*m.data[10]+data[7]*m.data[14],
				data[4]*m.data[3]+data[5]*m.data[7]+data[6]*m.data[11]+data[7]*m.data[15],

				data[8]*m.data[0]+data[9]*m.data[4]+data[10]*m.data[8]+data[11]*m.data[12],
				data[8]*m.data[1]+data[9]*m.data[5]+data[10]*m.data[9]+data[11]*m.data[13],
				data[8]*m.data[2]+data[9]*m.data[6]+data[10]*m.data[10]+data[11]*m.data[14],
				data[8]*m.data[3]+data[9]*m.data[7]+data[10]*m.data[11]+data[11]*m.data[15],

				data[12]*m.data[0]+data[13]*m.data[4]+data[14]*m.data[8]+data[15]*m.data[12],
				data[12]*m.data[1]+data[13]*m.data[5]+data[14]*m.data[9]+data[15]*m.data[13],
				data[12]*m.data[2]+data[13]*m.data[6]+data[14]*m.data[10]+data[15]*m.data[14],
				data[12]*m.data[3]+data[13]*m.data[7]+data[14]*m.data[11]+data[15]*m.data[15]);
		}
		public vec4 multiply(vec4 v) {
			return new vec4(
				data[0]*v.x+data[1]*v.y+data[2]*v.z+data[3]*v.w,
				data[4]*v.x+data[5]*v.y+data[6]*v.z+data[7]*v.w,
				data[8]*v.x+data[9]*v.y+data[10]*v.z+data[11]*v.w,
				data[12]*v.x+data[13]*v.y+data[14]*v.z+data[15]*v.w);
		}
		public mat4 multiply(float v) {
			return new mat4(
				data[0]*v, data[1]*v, data[2]*v, data[3]*v,
				data[4]*v, data[5]*v, data[6]*v, data[7]*v,
				data[8]*v, data[9]*v, data[10]*v, data[11]*v,
				data[12]*v, data[13]*v, data[14]*v, data[15]*v);
		}
		public mat4 transpose() {
			return new mat4(
				data[0], data[4], data[8], data[12],
				data[1], data[5], data[9], data[13],
				data[2], data[6], data[10], data[14],
				data[3], data[7], data[11], data[15]);
		}
	}
}
