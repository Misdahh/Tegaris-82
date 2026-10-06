extends Node3D

var p1:Fighter3D
var p2:Fighter3D
var cam:Camera3D
var arena:Node3D
var ui:CanvasLayer
var hp1:ProgressBar
var hp2:ProgressBar
var status:Label
var touch_move := 0.0

func _ready():
    _make_world(); _make_fighters(); _make_camera(); _make_ui()

func mat(c:Color, metallic:=0.0, rough:=0.7):
    var m=StandardMaterial3D.new(); m.albedo_color=c; m.metallic=metallic; m.roughness=rough; return m

func box(pos,size,color):
    var n=MeshInstance3D.new(); var m=BoxMesh.new(); m.size=size; n.mesh=m; n.position=pos; n.material_override=mat(color); arena.add_child(n); return n

func _make_world():
    arena=Node3D.new(); arena.name="TEGARIS82_TEMPLE_3D"; add_child(arena)
    box(Vector3(0,-0.18,0),Vector3(18,0.35,9),Color(0.08,0.07,0.07))
    for x in [-7.5,7.5]:
        for z in [-3.6,3.6]: box(Vector3(x,1.8,z),Vector3(0.65,3.6,0.65),Color(0.22,0.18,0.16))
    box(Vector3(0,4.0,3.7),Vector3(12,0.7,0.5),Color(0.28,0.20,0.12))
    box(Vector3(0,4.0,-3.7),Vector3(12,0.7,0.5),Color(0.28,0.20,0.12))
    var env=WorldEnvironment.new(); var e=Environment.new(); e.background_mode=Environment.BG_COLOR; e.background_color=Color(0.025,0.02,0.025); e.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR; e.ambient_light_color=Color(0.35,0.35,0.42); e.ambient_light_energy=0.65; env.environment=e; add_child(env)
    var light=DirectionalLight3D.new(); light.rotation_degrees=Vector3(-45,-25,0); light.light_energy=1.3; light.shadow_enabled=true; add_child(light)
    var fill=OmniLight3D.new(); fill.position=Vector3(0,4,1); fill.omni_range=12; fill.light_energy=4; fill.light_color=Color(0.35,0.5,1.0); add_child(fill)

func _make_fighters():
    p1=Fighter3D.new(); p1.player=true; p1.fighter_name="GARIS"; p1.body_color=Color(0.08,0.08,0.09); p1.accent_color=Color(0.8,0.05,0.04); p1.position=Vector3(-2.0,0,0); add_child(p1)
    p2=Fighter3D.new(); p2.player=false; p2.fighter_name="RIVAL"; p2.body_color=Color(0.12,0.08,0.16); p2.accent_color=Color(0.55,0.08,0.85); p2.position=Vector3(2.0,0,0); add_child(p2)
    p1.opponent=p2; p2.opponent=p1; p2.rotation.y=PI

func _make_camera():
    cam=Camera3D.new(); add_child(cam); cam.current=true; cam.fov=42

func _process(delta):
    if not p1 or not p2: return
    var mid=(p1.global_position+p2.global_position)*0.5
    cam.position=cam.position.lerp(mid+Vector3(0,2.8,7.6),delta*4.0)
    cam.look_at(mid+Vector3(0,1.25,0),Vector3.UP)
    if hp1: hp1.value=p1.hp
    if hp2: hp2.value=p2.hp
    if status: status.text="%s  %.0f HP     VS     %.0f HP  %s" % [p1.fighter_name,p1.hp,p2.hp,p2.fighter_name]

func _button(text_value:String, pos:Vector2, size:Vector2, action:String):
    var b=Button.new(); b.text=text_value; b.position=pos; b.size=size; b.modulate=Color(1,1,1,0.78); b.add_theme_font_size_override("font_size",22); b.pressed.connect(func(): _touch_action(action)); ui.add_child(b)

func _touch_action(action:String):
    if action=="left": touch_move=-1.0
    elif action=="right": touch_move=1.0
    elif action=="stop": touch_move=0.0
    elif action=="punch": p1.do_attack("punch")
    elif action=="kick": p1.do_attack("kick")
    elif action=="block": p1.blocking=true
    elif action=="jump" and p1.is_on_floor(): p1.velocity.y=6.5

func _make_ui():
    ui=CanvasLayer.new(); add_child(ui)
    hp1=ProgressBar.new(); hp1.position=Vector2(35,28); hp1.size=Vector2(420,28); hp1.max_value=100; hp1.value=100; ui.add_child(hp1)
    hp2=ProgressBar.new(); hp2.position=Vector2(825,28); hp2.size=Vector2(420,28); hp2.max_value=100; hp2.value=100; ui.add_child(hp2)
    status=Label.new(); status.position=Vector2(420,62); status.size=Vector2(450,35); status.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER; status.add_theme_font_size_override("font_size",20); ui.add_child(status)
    var title=Label.new(); title.text="TEGARIS82  •  TRUE 3D FIGHT"; title.position=Vector2(35,75); title.add_theme_font_size_override("font_size",24); ui.add_child(title)
    var hint=Label.new(); hint.text="A/D Move   J Punch   K Kick   L Block   SPACE Jump"; hint.position=Vector2(35,660); hint.add_theme_font_size_override("font_size",18); ui.add_child(hint)
    _button("◀",Vector2(40,570),Vector2(90,70),"left")
    _button("▶",Vector2(140,570),Vector2(90,70),"right")
    _button("PUNCH",Vector2(1010,500),Vector2(150,62),"punch")
    _button("KICK",Vector2(1010,570),Vector2(150,62),"kick")
    _button("JUMP",Vector2(840,570),Vector2(150,62),"jump")
    _button("BLOCK",Vector2(840,500),Vector2(150,62),"block")

func _input(event):
    if event is InputEventScreenTouch and not event.pressed:
        touch_move=0.0
        p1.blocking=false
