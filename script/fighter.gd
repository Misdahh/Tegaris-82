extends CharacterBody3D
class_name Fighter3D

@export var player := true
@export var fighter_name := "GARIS"
@export var body_color := Color(0.12,0.12,0.14)
@export var accent_color := Color(0.75,0.05,0.04)
var opponent: Fighter3D
var hp := 100.0
var speed := 3.8
var state := "idle"
var state_t := 0.0
var attack_t := 0.0
var hit_t := 0.0
var blocking := false
var base_y := 0.0
var root3d: Node3D
var torso: MeshInstance3D
var head: MeshInstance3D
var arms=[]
var legs=[]
var forearms=[]
var shins=[]

func _ready():
    base_y = position.y
    _build_body()

func mat(c:Color, metallic:=0.0, rough:=0.55):
    var m=StandardMaterial3D.new(); m.albedo_color=c; m.metallic=metallic; m.roughness=rough; return m

func capsule(name:String, radius:float, height:float, color:Color):
    var n=MeshInstance3D.new(); n.name=name
    var mesh=CapsuleMesh.new(); mesh.radius=radius; mesh.height=height; mesh.radial_segments=16; mesh.rings=8
    n.mesh=mesh; n.material_override=mat(color,0.1,0.45); return n

func sphere(name:String, radius:float, color:Color):
    var n=MeshInstance3D.new(); n.name=name
    var mesh=SphereMesh.new(); mesh.radius=radius; mesh.height=radius*2; mesh.radial_segments=20; mesh.rings=12
    n.mesh=mesh; n.material_override=mat(color,0.05,0.4); return n

func limb(a:Vector3,b:Vector3,r:float,color:Color):
    var n=capsule("limb",r,a.distance_to(b),color); n.position=(a+b)*0.5; n.look_at(b,Vector3.UP); n.rotate_object_local(Vector3.RIGHT,PI/2.0); return n

func _build_body():
    root3d=Node3D.new(); add_child(root3d)
    torso=capsule("Torso",0.48,1.25,body_color); torso.position.y=1.55; torso.scale=Vector3(1.0,1.0,0.68); root3d.add_child(torso)
    head=sphere("Head",0.33,Color(0.72,0.50,0.38)); head.position=Vector3(0,2.45,0); root3d.add_child(head)
    var hair=sphere("Hair",0.35,Color(0.025,0.02,0.018)); hair.position=Vector3(0,2.62,0); hair.scale=Vector3(1,0.6,1); root3d.add_child(hair)
    var shoulderL=Node3D.new(); shoulderL.name="ShoulderL"; root3d.add_child(shoulderL)
    var shoulderR=Node3D.new(); shoulderR.name="ShoulderR"; root3d.add_child(shoulderR)
    for side in [-1,1]:
        var upper=limb(Vector3(0.42*side,2.0,0),Vector3(0.72*side,1.48,0.02),0.15,body_color); root3d.add_child(upper); arms.append(upper)
        var fore=limb(Vector3(0.72*side,1.48,0.02),Vector3(0.78*side,0.98,0.05),0.13,accent_color); root3d.add_child(fore); forearms.append(fore)
        var glove=sphere("Glove",0.18,accent_color); glove.position=Vector3(0.78*side,0.88,0.05); root3d.add_child(glove)
        var thigh=limb(Vector3(0.22*side,1.0,0),Vector3(0.28*side,0.45,0),0.18,Color(0.08,0.08,0.09)); root3d.add_child(thigh); legs.append(thigh)
        var shin=limb(Vector3(0.28*side,0.45,0),Vector3(0.30*side,0.05,0.04),0.14,Color(0.10,0.10,0.12)); root3d.add_child(shin); shins.append(shin)
    var belt=MeshInstance3D.new(); belt.mesh=BoxMesh.new(); belt.mesh.size=Vector3(0.85,0.12,0.55); belt.position=Vector3(0,1.02,0); belt.material_override=mat(accent_color,0.2,0.3); root3d.add_child(belt)

func take_hit(dmg:float, heavy:=false):
    if blocking: dmg*=0.25
    hp=max(0,hp-dmg); hit_t=0.28 if not heavy else 0.42; state="hit"
    if hp<=0: state="down"

func do_attack(kind:String):
    if attack_t>0 or state=="down": return
    attack_t=0.46 if kind=="punch" else 0.62; state=kind
    if opponent and global_position.distance_to(opponent.global_position)<2.15:
        opponent.take_hit(10 if kind=="punch" else 14, kind=="kick")

func _physics_process(delta):
    state_t+=delta
    if hit_t>0: hit_t-=delta
    if attack_t>0: attack_t-=delta
    if state=="down": return
    if player:
        var dir=Input.get_axis("move_left","move_right")
        var owner_node=get_parent()
        if owner_node and "touch_move" in owner_node: dir=owner_node.touch_move
        velocity.x=dir*speed
        blocking=Input.is_action_pressed("block") and attack_t<=0
        if Input.is_action_just_pressed("punch"): do_attack("punch")
        if Input.is_action_just_pressed("kick"): do_attack("kick")
        if Input.is_action_just_pressed("jump") and is_on_floor(): velocity.y=6.5
    else:
        var dx=opponent.global_position.x-global_position.x
        velocity.x=clamp(sign(dx)*speed*0.62,-2.4,2.4)
        blocking=false
        if abs(dx)<1.9 and attack_t<=0 and randf()<delta*1.8: do_attack("punch" if randf()<0.55 else "kick")
    if not is_on_floor(): velocity.y-=16*delta
    move_and_slide()
    position.y=max(position.y,base_y)
    _animate(delta)

func _animate(delta):
    var t=Time.get_ticks_msec()/1000.0
    var bob=sin(t*3.0)*0.018 if state=="idle" else 0.0
    torso.position.y=1.55+bob
    head.position.y=2.45+bob
    if hit_t>0: root3d.rotation.z=sin(hit_t*22)*0.12
    else: root3d.rotation.z=0
    if attack_t>0:
        var p=1.0-attack_t/(0.46 if state=="punch" else 0.62)
        if state=="punch": forearms[1].rotation.z=-1.0+min(p*2.5,1.6)
        if state=="kick": legs[1].rotation.z=sin(min(p,1.0)*PI)*1.1
    else:
        forearms[0].rotation.z=0; forearms[1].rotation.z=0; legs[0].rotation.z=0; legs[1].rotation.z=0
