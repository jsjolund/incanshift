# Creates a CSV file listing the names of each blender object 
# in the .blend along with its position and rotation.
# The file has the same name as the .blend file but with .csv
# extension.
#
# For each model which name does NOT contain a number it also
# exports the model to a .obj file for importing into LibGDX.
#
# E.g. 'box.001', 'box.002' would NOT get exported, but 'box'
# will be exported to 'box.obj' and 'box.mtl'.

import bpy
import os
import math
import subprocess

performFbxConv = true;
performObjConv = true;

excludeNames = ['start_position', 'text_tag']
excludeTypes = [bpy.types.Camera, bpy.types.PointLamp]

def main():
	print("\nStarting export.")
	basedir = os.path.dirname(bpy.data.filepath)
	filename = bpy.path.basename(bpy.context.blend_data.filepath).split(".")[0]
	scene = bpy.context.scene
	if not basedir:
		raise Exception("Blend file is not saved")
	fn = os.path.join(basedir, filename + ".csv")
	text_file = open(fn, "w")
	csv = "\nname;loc.x;loc.y;loc.z;rot.x;rot.y;rot.z\n"
	print("\nExporting models:")
	for obj in bpy.data.objects:
		if type(obj.data) not in excludeTypes:
			# Construct and write a row for the object in the CSV file
			names = str(obj.name).split(".")
			name = names[0]
			euler_rotation = obj.rotation_euler
			rot = [math.degrees(a) for a in euler_rotation]
			loc = obj.location
			objrow = "{};{:.2f};{:.2f};{:.2f};{:.2f};{:.2f};{:.2f}\n".format(name, loc.x, loc.y, loc.z, rot[0], rot[1], rot[2])
			csv += objrow
			text_file.write(objrow)
			
			# Export unique object to .obj
			if (len(names) > 1) or (name in excludeNames) or (not performObjConv):
				continue
			
			loc = obj.location
			e_rot = obj.rotation_euler
			old_loc = loc.copy()
			old_e_rot = e_rot.copy()
			loc.zero()
			e_rot.zero()
			
			obj.select = True
			scene.objects.active = obj
			fn = os.path.join(basedir, name)
			bpy.ops.export_scene.obj(filepath=fn + ".obj", use_selection=True)
			obj.select = False
		
			loc.x = old_loc.x
			loc.y = old_loc.y
			loc.z = old_loc.z
			e_rot.x = old_e_rot.x
			e_rot.y = old_e_rot.y
			e_rot.z = old_e_rot.z

	text_file.close()
	
	if performFbxConv and performObjConv:
		print("\nConverting .obj and .mtl to .g3db:")
		for subdir, dirs, files in os.walk(basedir):
			for filename in files:
				file_no_ext, file_ext = os.path.splitext(filename)
				if file_ext == '.obj':
					objfile_path = os.path.join(basedir, filename)
					mtlfile_path = os.path.join(basedir, file_no_ext+".mtl")
					subprocess.call(["fbx-conv", objfile_path])
					os.remove(objfile_path)
					os.remove(mtlfile_path)
					print("\nRemoved:\n{}\n{}\n".format(objfile_path, mtlfile_path))

	print("Wrote to to {}.csv".format(fn))
	print(csv)
	print("Success.")
	
if __name__ == "__main__":
	main()
