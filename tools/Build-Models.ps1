param([string]$Atlas = "$PSScriptRoot/../docs/art/subnet-atlas-source.png")
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assets = [IO.Path]::GetFullPath("$PSScriptRoot/../common/src/main/resources/assets/me_subnet_bus")
$textureDir = New-Item -ItemType Directory -Force "$assets/textures/part"
$source = [Drawing.Bitmap]::new([IO.Path]::GetFullPath($Atlas))
try {
    $names = @('storage','interface','power','edge')
    for ($tile = 0; $tile -lt 4; $tile++) {
        $bitmap = [Drawing.Bitmap]::new(16,16)
        try {
            for ($y = 0; $y -lt 16; $y++) {
                for ($x = 0; $x -lt 16; $x++) {
                    $sx = [int][Math]::Floor((($tile % 2) + ($x + 0.5)/16) * $source.Width/2)
                    $sy = [int][Math]::Floor(([Math]::Floor($tile/2) + ($y + 0.5)/16) * $source.Height/2)
                    $bitmap.SetPixel($x,$y,$source.GetPixel($sx,$sy))
                }
            }
            $bitmap.Save("$textureDir/$($names[$tile]).png",[Drawing.Imaging.ImageFormat]::Png)
        } finally { $bitmap.Dispose() }
    }
} finally { $source.Dispose() }

function Face($texture,$uv = @(0,0,16,16)) { @{ texture = "#$texture"; uv = $uv } }
function Elements([int]$state,[double]$offset) {
    $mode = [int][Math]::Floor($state/2)
    $reversed = ($state % 2) -eq 1
    $outer = if ($mode -eq 0) {'power'} elseif ($reversed) {'storage'} else {'interface'}
    $inner = if ($mode -eq 0) {'power'} elseif ($reversed) {'interface'} else {'storage'}
    @{ from = @(4,4,$offset); to = @(12,12,($offset+3)); faces = @{
        north=(Face $outer); south=(Face $inner); east=(Face 'edge'); west=(Face 'edge'); up=(Face 'edge'); down=(Face 'edge')
    }}
    if ($mode -lt 2) { return }
    # A pixel-built white arrow sits on the green face, independent of the back face UV.
    $arrow = @('..#....','..##...','..###..','..####.','..###..','..##...','..#....')
    for ($y=0;$y -lt 7;$y++) { for ($x=0;$x -lt 7;$x++) {
        if ($arrow[$y][$x] -ne '#') { continue }
        $px = if ($mode -eq 3) {6-$x} else {$x}
        if ($reversed) { $px=6-$px }
        $left = 6.25+$px*0.5
        $bottom = 9.75-$y*0.5
        $z = if ($reversed) {$offset-0.005} else {$offset+3.005}
        $side = if ($reversed) {'north'} else {'south'}
        @{ from=@($left,($bottom-0.5),$z); to=@(($left+0.5),$bottom,$z); shade=$false; faces=@{ $side=(Face 'power' @(4,7,5,8)) } }
    }}
}
$textures = @{}
foreach ($name in @('storage','interface','power','edge')) { $textures[$name]="me_subnet_bus:part/$name" }
$textures.particle = 'me_subnet_bus:part/edge'
$display = @{
    gui=@{rotation=@(30,45,0);translation=@(0,0,0);scale=@(1.6,1.6,1.6)}
    ground=@{rotation=@(0,0,0);translation=@(0,3,0);scale=@(0.5,0.5,0.5)}
    fixed=@{rotation=@(0,180,0);translation=@(0,0,0);scale=@(1.4,1.4,1.4)}
    firstperson_righthand=@{rotation=@(0,-45,25);translation=@(0,2,0);scale=@(0.8,0.8,0.8)}
    firstperson_lefthand=@{rotation=@(0,45,-25);translation=@(0,2,0);scale=@(0.8,0.8,0.8)}
    thirdperson_righthand=@{rotation=@(75,45,0);translation=@(0,2.5,0);scale=@(0.5,0.5,0.5)}
    thirdperson_lefthand=@{rotation=@(75,-45,0);translation=@(0,2.5,0);scale=@(0.5,0.5,0.5)}
}
for ($state=0;$state -lt 8;$state++) {
    $part = @{ textures=$textures; elements=@(Elements $state 0) }
    $item = @{ textures=$textures; elements=@(Elements $state 6.5); display=$display; gui_light='front' }
    [IO.File]::WriteAllText("$assets/models/part/bus_$state.json",($part|ConvertTo-Json -Depth 20))
    [IO.File]::WriteAllText("$assets/models/item/bus_$state.json",($item|ConvertTo-Json -Depth 20))
}
