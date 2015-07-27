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


class GameObject(object):
    exclude_names = ['start_position', 'text_tag', 'fog_tag', 'sun_tag', 'sound_tag', 'mask']
    csv_header = "name;index;loc.x;loc.y;loc.z;rot.x;rot.y;rot.z"

    def get_first_name(self):
        return self.name_array[0]

    def get_model_name(self):
        if self.has_excluded_name():
            return self.get_first_name()
        return self.filename + "_" + self.get_first_name()

    def get_csv_row(self):
        return "{};{:};{:.2f};{:.2f};{:.2f};{:.2f};{:.2f};{:.2f}\n".format(self.get_model_name(), self.get_index(),
                                                                           self.loc.x, self.loc.y, self.loc.z,
                                                                           self.rot[0], self.rot[1], self.rot[2])

    def get_index(self):
        if (len(self.name_array) > 1):
            return int(self.name_array[1])
        else:
            return 0

    def __init__(self, filename, bobj):
        self.filename = filename
        self.bobj = bobj
        self.name_array = str(bobj.name).split(".")
        self.loc = bobj.location.copy()
        self.rote = bobj.rotation_euler.copy()
        self.rot = [math.degrees(a) for a in bobj.rotation_euler]

    def is_empty(self):
        return self.bobj.data is None

    def is_mesh(self):
        return type(self.bobj.data) is bpy.types.Mesh

    def has_excluded_name(self):
        for exclude_name in GameObject.exclude_names:
            if self.get_first_name().startswith(exclude_name):
                return True
        return False

    def is_exportable(self):
        is_empty = self.bobj.data is None
        is_mesh = type(self.bobj.data) is bpy_types.Mesh


def write_csv(csv_file_path, gobj_map):
    csv_file = open(csv_file_path, "w")
    print(GameObject.csv_header)
    print("-" * len(GameObject.csv_header))
    for name, gobj_list in gobj_map.items():
        for gobj in gobj_list:
            csv_file.write(gobj.get_csv_row())
            print(str(gobj.get_csv_row()).strip("\r\n"))
    csv_file.close()


def write_obj(obj_dir, export_objects):
    obj_file_paths = []

    # unselect all
    for item in bpy.context.selectable_objects:
        item.select = False

    for gobj0 in export_objects:
        bobj0 = gobj0.bobj
        obj_file_path = os.path.join(obj_dir, gobj0.get_model_name() + ".obj")
        obj_file_paths.append(obj_file_path)

        # Select object, set loc & rot to zero, export to obj, restore loc & rot, unselect
        bobj0.select = True
        bpy.context.scene.objects.active = bobj0
        bobj0.location.zero()
        bobj0.rotation_euler.zero()
        bpy.ops.export_scene.obj(filepath=obj_file_path, use_selection=True)
        bobj0.location = gobj0.loc.copy()
        bobj0.rotation_euler = gobj0.rote.copy()
        bobj0.select = False

    return obj_file_paths


def convert_to_g3db(obj_file_paths):
    g3db_file_paths = []
    for obj_file_path in obj_file_paths:
        subprocess.call(["fbx-conv", obj_file_path])
        file_path_noext, file_ext = os.path.splitext(obj_file_path)
        os.remove(file_path_noext + ".obj")
        os.remove(file_path_noext + ".mtl")
        g3db_file_paths.append(file_path_noext + ".g3db")


def get_export_objects(gobj_map):
    export_objects = []
    for name, gobj_list in gobj_map.items():
        if len(gobj_list) == 0:
            print("WARNING: No instances found for {}, using ".format(name))
        gobj0 = gobj_list[0]
        if gobj0.has_excluded_name():
            print("INFO: {} is in exclusion list.".format(name))
            continue
        if not gobj0.is_mesh():
            print("INFO: {} is not a mesh.".format(name))
            continue
        gobj0_candidates = []
        for gobj in gobj_list:
            if gobj.get_index() == 0:
                gobj0_candidates.append(gobj)
        if len(gobj0_candidates) > 1:
            print("WARNING: Multiple base models found for {}, using: {}".format(name, gobj0.bobj.name))
        if len(gobj0_candidates) == 0:
            print("WARNING: No base model found for {}, using: {}".format(name, gobj0.bobj.name))
        export_objects.append(gobj0)
    return export_objects


def create_game_object_map(filename):
    gobj_map = {}
    for obj in bpy.data.objects:
        gobj = GameObject(filename, obj)

        # Create map over names and objects with those names.
        # Allow meshes and empties.
        if gobj.is_mesh() or gobj.is_empty():
            name = gobj.get_first_name()
            if name in gobj_map:
                gobj_map.get(name).append(gobj)
            else:
                gobj_map[name] = [gobj]
    return gobj_map


def main():
    print("\nStarting level export...")
    basedir = os.path.dirname(bpy.data.filepath)
    filename = bpy.path.basename(bpy.context.blend_data.filepath).split(".")[0]
    scene = bpy.context.scene
    if not basedir:
        raise Exception("Blend file is not saved")

    gobj_map = create_game_object_map(filename)

    csv_file_path = os.path.join(basedir, filename + ".csv")
    print("\nWriting to " + csv_file_path)
    print()
    write_csv(csv_file_path, gobj_map)
    print()
    export_objects = get_export_objects(gobj_map);
    print()
    obj_file_paths = write_obj(basedir, export_objects)
    print()
    g3db_file_paths = convert_to_g3db(obj_file_paths)
    print("\nFinished.")


if __name__ == "__main__":
    main()
