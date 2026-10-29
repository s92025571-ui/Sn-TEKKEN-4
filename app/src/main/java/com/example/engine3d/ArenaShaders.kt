package com.example.engine3d

import android.opengl.GLES20

object ArenaShaders {

    const val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec2 aUV;
        attribute float aMaterialId;
        attribute float aAO;
        
        varying vec3 vWorldPos;
        varying vec3 vLocalPos;
        varying vec3 vWorldNormal;
        varying vec2 vUV;
        varying float vMaterialId;
        varying float vAO;
        
        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vWorldPos = worldPos.xyz;
            vLocalPos = aPosition;
            vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
            vUV = aUV;
            vMaterialId = aMaterialId;
            vAO = aAO;
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
        }
    """

    const val FRAGMENT_SHADER = """
        precision mediump float;
        
        uniform vec3 uCameraPos;
        uniform float uTime;
        uniform vec3 uTintFlash;      // Hit flash or telegraph aura RGB
        uniform float uFlashAmount;   // 0..1
        uniform float uAlpha;         // Transparency for shadows & particles
        uniform float uFloorGlowOn;   // 1.0 when floor neon circuit grid is active
        uniform float uBossPhase2;    // 1.0 when Dragon Boss enters Inferno Phase 2
        
        varying vec3 vWorldPos;
        varying vec3 vLocalPos;
        varying vec3 vWorldNormal;
        varying vec2 vUV;
        varying float vMaterialId;
        varying float vAO;
        
        void main() {
            int mat = int(vMaterialId + 0.5);
            
            // Soft Contact Shadow Pass
            if (mat == 18) {
                float dist = length(vUV - vec2(0.5, 0.5)) * 2.0;
                float softAlpha = clamp(1.0 - dist * dist, 0.0, 1.0) * uAlpha;
                gl_FragColor = vec4(0.02, 0.02, 0.04, softAlpha);
                return;
            }
            
            // Glowing Combat Particle Pass
            if (mat == 19) {
                gl_FragColor = vec4(uTintFlash, uAlpha);
                return;
            }
            
            // 3D Arena Floor Boss Telegraph Danger Zone Pass (uFlashAmount = telegraphProgress 0..1)
            if (mat == 20) {
                float radial = length(vUV - vec2(0.5, 0.5)) * 2.0;
                if (radial > 1.0) discard;
                float outerRim = smoothstep(0.84, 0.98, radial);
                float chargeWave = smoothstep(uFlashAmount + 0.06, uFlashAmount - 0.08, radial);
                float chevron = 0.5 + 0.5 * sin(radial * 20.0 - uTime * 14.0);
                float alphaZone = clamp(outerRim * 0.90 + chargeWave * 0.45 + chevron * 0.14, 0.0, 0.92) * uAlpha;
                vec3 zoneCol = mix(uTintFlash, vec3(1.0, 0.92, 0.55), outerRim * 0.55);
                gl_FragColor = vec4(zoneCol, alphaZone);
                return;
            }
            
            // 3D Volumetric Dragon Fire-Breath Cone & Stomp Shockwave Pass
            if (mat == 21) {
                float core = 0.55 + 0.45 * sin(vUV.y * 22.0 - uTime * 28.0 + sin(vUV.x * 18.0) * 2.0);
                vec3 hotCore = vec3(1.0, 0.90, 0.42);
                vec3 flameCol = mix(uTintFlash, hotCore, core * (1.0 - vUV.y * 0.55));
                float fadeTip = clamp(1.15 - vUV.y, 0.0, 1.0);
                gl_FragColor = vec4(flameCol, fadeTip * uAlpha);
                return;
            }
            
            vec3 N = normalize(vWorldNormal);
            vec3 V = normalize(uCameraPos - vWorldPos);
            
            // Studio 3-Point Underground Combat Arena Lighting
            vec3 keyLightDir = normalize(vec3(0.35, 0.85, 0.58));
            vec3 rimLightDir = normalize(vec3(-0.65, 0.45, -0.70));
            vec3 bounceDir   = normalize(vec3(0.0, -1.0, 0.25));
            
            float diffKey = max(dot(N, keyLightDir), 0.0);
            float diffRim = max(dot(N, rimLightDir), 0.0);
            float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.6);
            
            vec3 baseColor = vec3(0.5);
            float shininess = 24.0;
            float specStrength = 0.28;
            vec3 emissive = vec3(0.0);
            
            if (mat == 1) {
                // SHADOW TIGER FUR: Rich orange-gold with organic procedural black tiger stripes!
                vec3 deepOrange = vec3(0.88, 0.36, 0.06);
                vec3 brightGold = vec3(1.00, 0.58, 0.16);
                baseColor = mix(deepOrange, brightGold, clamp(N.y * 0.5 + 0.5, 0.0, 1.0));
                float warp = sin(vLocalPos.x * 18.0 + vLocalPos.z * 14.0) * 1.8;
                float stripeWave = sin(vLocalPos.y * 34.0 + warp);
                float stripeMask = smoothstep(0.52, 0.76, stripeWave);
                baseColor = mix(baseColor, vec3(0.07, 0.05, 0.04), stripeMask * 0.90);
                // Velvety fur rim sheen
                emissive += vec3(0.32, 0.14, 0.03) * fresnel;
                specStrength = 0.18;
            } else if (mat == 2) {
                // TIGER CREAM RUFF & MUZZLE
                baseColor = vec3(0.95, 0.89, 0.78);
                float whiskerDots = smoothstep(0.75, 0.92, sin(vLocalPos.x * 65.0) * sin(vLocalPos.y * 65.0));
                baseColor = mix(baseColor, vec3(0.20, 0.14, 0.10), whiskerDots * 0.45);
                specStrength = 0.15;
            } else if (mat == 3) {
                // WET ANIMAL NOSE PAD
                baseColor = vec3(0.14, 0.11, 0.12);
                shininess = 56.0;
                specStrength = 0.65;
            } else if (mat == 4) {
                // FIERCE GLOWING WARRIOR EYES
                baseColor = vec3(1.0, 0.72, 0.10);
                emissive = vec3(1.0, 0.62, 0.08) * 0.95;
                shininess = 80.0;
                specStrength = 0.90;
            } else if (mat == 5) {
                // FANGS, CLAWS & TALONS
                baseColor = vec3(0.94, 0.91, 0.83);
                shininess = 48.0;
                specStrength = 0.50;
            } else if (mat == 6) {
                // CRIMSON-OBSIDIAN MARTIAL ARTS GI FABRIC
                float weave = sin(vUV.x * 80.0 + vUV.y * 80.0) * 0.04;
                baseColor = mix(vec3(0.46, 0.07, 0.08), vec3(0.14, 0.05, 0.07), vUV.y * 0.55) + weave;
                specStrength = 0.14;
                shininess = 14.0;
            } else if (mat == 7) {
                // GOLDEN EMBROIDERED MARTIAL BELT & TRIM
                baseColor = vec3(0.85, 0.64, 0.16);
                shininess = 64.0;
                specStrength = 0.78;
            } else if (mat == 8) {
                // MARTIAL FOREARM & SHIN CLOTH WRAPS
                float band = smoothstep(0.65, 0.90, abs(sin(vUV.y * 28.0)));
                baseColor = mix(vec3(0.76, 0.71, 0.63), vec3(0.48, 0.43, 0.37), band * 0.45);
                specStrength = 0.10;
            } else if (mat == 9) {
                // IRON BUFFALO COARSE DARK HIDE
                float hair = sin(vLocalPos.x * 52.0 + sin(vLocalPos.y * 30.0) * 2.0) * 0.03;
                baseColor = vec3(0.15, 0.13, 0.12) + hair;
                emissive += vec3(0.08, 0.13, 0.18) * fresnel;
                specStrength = 0.22;
            } else if (mat == 10) {
                // RIDGED KERATIN HORNS
                float rings = sin(vUV.y * 42.0) * 0.07;
                baseColor = mix(vec3(0.20, 0.19, 0.18), vec3(0.66, 0.62, 0.54), vUV.y) + rings;
                shininess = 44.0;
                specStrength = 0.48;
            } else if (mat == 11) {
                // BRUSHED GUNMETAL IRON ARMOR & GAUNTLETS
                float scratch = sin(vLocalPos.x * 45.0 - vLocalPos.y * 22.0) * 0.04;
                baseColor = vec3(0.36, 0.41, 0.48) + scratch;
                shininess = 78.0;
                specStrength = 0.95;
                emissive += vec3(0.08, 0.18, 0.28) * fresnel;
            } else if (mat == 12) {
                // RUGGED BATTLE LEATHER STRAPS
                baseColor = vec3(0.34, 0.20, 0.11);
                shininess = 28.0;
                specStrength = 0.30;
            } else if (mat == 13) {
                // DRAGON BOSS ARMORED SCALES
                float s1 = sin(vLocalPos.x * 32.0 + vLocalPos.y * 32.0);
                float s2 = sin(vLocalPos.x * 32.0 - vLocalPos.y * 32.0);
                float scalePattern = smoothstep(-0.3, 0.7, s1 * s2);
                vec3 obsidian = vec3(0.14, 0.05, 0.06);
                vec3 crimsonScale = vec3(0.56, 0.09, 0.08);
                baseColor = mix(obsidian, crimsonScale, scalePattern);
                shininess = 62.0;
                specStrength = 0.68;
                emissive += vec3(0.45, 0.10, 0.03) * fresnel * (1.0 + uBossPhase2 * 0.8);
            } else if (mat == 14) {
                // DRAGON MOLTEN MAGMA CHEST & THROAT
                float pulse = 0.78 + 0.22 * sin(uTime * 5.5 + vLocalPos.y * 6.0);
                baseColor = vec3(0.95, 0.34, 0.06);
                emissive = vec3(1.0, 0.42, 0.08) * pulse * (0.65 + uBossPhase2 * 0.55);
            } else if (mat == 15) {
                // DRAGON WING MEMBRANE
                float veins = smoothstep(0.80, 0.98, abs(sin(vLocalPos.x * 16.0 + vLocalPos.y * 10.0)));
                baseColor = mix(vec3(0.28, 0.05, 0.06), vec3(0.75, 0.18, 0.06), veins * 0.55);
                emissive = vec3(0.22, 0.04, 0.02) * (0.5 + uBossPhase2 * 0.6);
            } else if (mat == 16) {
                // FUTURISTIC UNDERGROUND ARENA OCTAGON FLOOR
                float r = length(vWorldPos.xz);
                float tileGrid = smoothstep(0.94, 0.99, abs(sin(vWorldPos.x * 2.4))) +
                                 smoothstep(0.94, 0.99, abs(sin(vWorldPos.z * 2.4)));
                baseColor = vec3(0.08, 0.10, 0.14) + vec3(0.03, 0.04, 0.06) * clamp(tileGrid, 0.0, 1.0);
                // Spotlight pool in center of combat stage
                float spot = clamp(1.0 - (r / 6.8), 0.0, 1.0);
                baseColor *= (0.45 + 0.85 * spot);
                if (uFloorGlowOn > 0.5) {
                    float innerRing = smoothstep(0.14, 0.0, abs(r - 3.6));
                    float outerRing = smoothstep(0.18, 0.0, abs(r - 5.2));
                    vec3 ringColor = mix(vec3(0.08, 0.68, 0.92), vec3(1.0, 0.42, 0.08), uBossPhase2);
                    emissive += ringColor * innerRing * 0.65 + vec3(0.95, 0.45, 0.10) * outerRing * 0.50;
                }
                shininess = 52.0;
                specStrength = 0.42;
            } else {
                // ARENA STADIUM PILLARS & NEON TRUSSES
                baseColor = vec3(0.10, 0.12, 0.17);
                float neonStrip = smoothstep(0.86, 0.96, sin(vWorldPos.y * 3.2));
                vec3 stripCol = mix(vec3(0.12, 0.72, 0.98), vec3(1.0, 0.35, 0.08), step(0.0, vWorldPos.x));
                emissive += stripCol * neonStrip * 0.45;
                shininess = 48.0;
                specStrength = 0.50;
            }
            
            // Blinn-Phong Specular
            vec3 H = normalize(keyLightDir + V);
            float spec = pow(max(dot(N, H), 0.0), shininess) * specStrength;
            
            // Combine Hemisphere Ambient + Key Warm + Cool Cyan Rim + Arena Bounce
            vec3 ambient = mix(vec3(0.14, 0.16, 0.24), vec3(0.28, 0.28, 0.34), N.y * 0.5 + 0.5) * vAO;
            vec3 keyCol  = vec3(1.02, 0.96, 0.88) * diffKey * 0.88;
            vec3 rimCol  = vec3(0.28, 0.65, 0.95) * diffRim * 0.42 + vec3(0.22, 0.50, 0.82) * fresnel * 0.30;
            vec3 bounce  = vec3(0.35, 0.16, 0.08) * max(dot(N, -bounceDir), 0.0) * 0.18;
            
            vec3 litColor = baseColor * (ambient + keyCol + bounce) + rimCol + vec3(spec) + emissive;
            
            // Apply Hit Flash or Boss Telegraph Warning Tint
            litColor = mix(litColor, uTintFlash, clamp(uFlashAmount, 0.0, 0.85));
            
            // Distance Atmospheric Arena Fog for depth
            float camDist = length(uCameraPos - vWorldPos);
            float fogFactor = clamp((camDist - 9.0) / 14.0, 0.0, 0.78);
            vec3 fogColor = mix(vec3(0.04, 0.05, 0.08), vec3(0.14, 0.04, 0.03), uBossPhase2 * 0.6);
            litColor = mix(litColor, fogColor, fogFactor);
            
            gl_FragColor = vec4(litColor, uAlpha);
        }
    """

    fun createProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
}
